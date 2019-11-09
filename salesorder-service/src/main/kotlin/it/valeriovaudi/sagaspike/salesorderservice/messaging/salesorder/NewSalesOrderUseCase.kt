package it.valeriovaudi.sagaspike.salesorderservice.messaging.salesorder

import it.valeriovaudi.sagaspike.salesorderservice.GoodsRepository
import it.valeriovaudi.sagaspike.salesorderservice.messaging.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor
import org.springframework.integration.annotation.Gateway
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.dsl.EnricherSpec
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.dsl.MessageChannels
import org.springframework.integration.dsl.RouterSpec
import org.springframework.integration.redis.store.RedisMessageStore
import org.springframework.integration.router.ExpressionEvaluatingRouter
import org.springframework.integration.store.MessageGroup
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.handler.annotation.Payload
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import reactor.core.scheduler.Schedulers
import java.util.*

@MessagingGateway
interface NewSalesOrderGateway {

    @Gateway(requestChannel = "newSalesOrderRequestChannel", replyChannel = "newSalesOrderResponseChannel")
    fun newSalesOrder(@Payload newSalesOrderRequest: NewSalesOrderRequest): Mono<String>

}


@Configuration
class NewSalesOrderUseCaseConfig {

    @Bean
    fun newSalesOrder(newSalesOrderRequestChannel: MessageChannel,
                      newSalesOrderResponseChannel: MessageChannel,
                      salesOrderMessageChannel: SalesOrderMessageChannel) =
            IntegrationFlows.from(newSalesOrderRequestChannel)
                    .publishSubscribeChannel { channel ->
                        channel.subscribe { flow ->
                            flow.transform("payload.salesOrderId")
                                    .channel(newSalesOrderResponseChannel)
                        }

                        channel.subscribe { flow ->
                            flow.enrichHeaders(mapOf("execution-id" to UUID.randomUUID().toString()))
                                    .channel(salesOrderMessageChannel.createSalesOrderRequestChannel())
                        }
                    }
                    .get()


    @Bean
    fun createSalesOrderUseCaseSplittator(createSalesOrderResponseChannel: MessageChannel,
                                          catalogMessageChannel: CatalogMessageChannel) =
            IntegrationFlows.from(createSalesOrderResponseChannel)
                    .split()
                    .enrich { t: EnricherSpec -> t.headerExpression("goods-quantity", "payload.quantity") }
                    .enrich { t: EnricherSpec -> t.headerExpression("sales-order-id", "headers['sales-order-id']") }
                    .transform { source: GoodsRequest -> GoodsPriceMessageRequest("CATALOG01", source.barcode) }
                    .channel(catalogMessageChannel.goodsPricingRequestChannel())
                    .get()

    @Bean
    fun createSalesOrderUseCaseAggregator(goodsRepository: GoodsRepository,
                                          responseChannelAdapter: MessageChannel,
                                          catalogMessageChannel: CatalogMessageChannel,
                                          redisMessageStore: RedisMessageStore) =
            IntegrationFlows.from(responseChannelAdapter)
                    .aggregate { aggregatorSpec ->
                        aggregatorSpec
                                .messageStore(redisMessageStore)
                                .outputProcessor(SalesOrderGoodsAggregator())
                    }
                    .route("headers['goods-to-remove']") { t: RouterSpec<String, ExpressionEvaluatingRouter> ->
                        t.channelMapping("false", "processSalesOrderGoods")
                                .channelMapping("true", "rollbackSalesOrderGoods")
                    }
                    .get()

}

class SalesOrderGoodsAggregator : DefaultAggregatingMessageGroupProcessor() {

    override fun aggregateHeaders(group: MessageGroup): MutableMap<String, Any> {
        val aggregateHeaders = super.aggregateHeaders(group)
        val aggregation = group.messages.stream().anyMatch(::haveToRollback).or(false)
        aggregateHeaders.put("goods-to-remove", aggregation)
        return aggregateHeaders
    }
}

private fun haveToRollback(message: Message<*>) =
        message.headers.getOrDefault("goods-to-remove", false) as Boolean


@Configuration
class NewSalesOrderProcessingPipelineConfig {

    @Bean
    fun rollbackSalesOrderGoodsPipeline(goodsRepository: GoodsRepository,
                                        rollbackSalesOrderGoods: MessageChannel,
                                        inventoryMessageChannel: InventoryMessageChannel,
                                        catalogMessageChannel: CatalogMessageChannel,
                                        redisMessageStore: RedisMessageStore) =
            IntegrationFlows.from(rollbackSalesOrderGoods)
                    .handle { goods: List<SalesOrderGoodsMessageWrapper>, headers: MessageHeaders ->

                        println("rollback goods")
                        println(goods)
                        goods.toFlux()
                                .filter { wrapper -> wrapper.salesOrderGoods.id != null }
                                .flatMap { wrapper ->
                                    wrapper.salesOrderGoods.let {
                                        goodsRepository.delete(it).thenMany(wrapper.toMono())
                                    }
                                }
                                .filter { t -> !t.hasRollback }
                                .flatMap { wrapper ->
                                    wrapper.salesOrderGoods.let { ReserveGoodsMessage(it.barcode, it.quantity) }.toMono()
                                }
                    }
                    .channel(MessageChannels.flux())
                    .split()
                    .channel(inventoryMessageChannel.unReserveGoodsRequestChannel())
                    .get()


    @Bean
    fun processSalesOrderGoodsPipeline(goodsRepository: GoodsRepository,
                                       processSalesOrderGoods: MessageChannel,
                                       catalogMessageChannel: CatalogMessageChannel,
                                       redisMessageStore: RedisMessageStore) =
            IntegrationFlows.from(processSalesOrderGoods)
                    .log()
                    .handle { goods: List<SalesOrderGoodsMessageWrapper> ->
                        println("processSalesOrderGoods goods")
                        goods.map { it.salesOrderGoods }
                                .let {
                                    goodsRepository.saveAll(it)
                                            .subscribeOn(Schedulers.elastic())
                                            .subscribe()
                                }
                        Unit
                    }.get()
}
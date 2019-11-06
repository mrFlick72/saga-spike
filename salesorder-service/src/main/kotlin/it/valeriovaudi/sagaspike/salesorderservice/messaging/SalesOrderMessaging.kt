package it.valeriovaudi.sagaspike.salesorderservice.messaging

import it.valeriovaudi.sagaspike.salesorderservice.*
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.Output
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.reactive.FluxSender
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
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
import org.springframework.messaging.SubscribableChannel
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.support.MessageBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.core.scheduler.Schedulers
import java.util.*

data class NewSalesOrderRequest(var salesOrderId: String? = null, var customer: CustomerRepresentation, var goods: List<GoodsRequest> = emptyList()) {
    constructor() : this(UUID.randomUUID().toString(), CustomerRepresentation("", ""), emptyList())
}

data class GoodsRequest(var barcode: String, var quantity: Int) {
    constructor() : this(barcode = "", quantity = 0)
}

@MessagingGateway
interface NewSalesOrderGateway {

    @Gateway(requestChannel = "newSalesOrderRequestChannel", replyChannel = "newSalesOrderResponseChannel")
    fun newSalesOrder(@Payload newSalesOrderRequest: NewSalesOrderRequest): Mono<String>

}

interface SalesOrderMessageChannel {

    @Input
    fun createSalesOrderRequestChannel(): SubscribableChannel

}

@Configuration
class CreateSalesOrderUseCaseConfig {

    @Bean
    fun salesOrderCompleteChannel() = MessageChannels.flux()

    @Bean
    fun responseChannelAdapter() = MessageChannels.flux()

    @Bean
    fun createSalesOrderResponseChannel() = MessageChannels.flux()

    @Bean
    fun processSalesOrderGoods() = MessageChannels.flux();

    @Bean
    fun rollbackSalesOrderGoods() = MessageChannels.flux();

    @Bean
    fun redisMessageStore(redisConnectionFactory: RedisConnectionFactory) =
            RedisMessageStore(redisConnectionFactory)

    @Bean
    fun createSalesOrderUseCaseSplittator(catalogMessageChannel: CatalogMessageChannel) =
            IntegrationFlows.from(createSalesOrderResponseChannel())
                    .split()
                    .enrich { t: EnricherSpec -> t.headerExpression("goods-quantity", "payload.quantity") }
                    .enrich { t: EnricherSpec -> t.headerExpression("sales-order-id", "headers['sales-order-id']") }
                    .transform { source: GoodsRequest -> GoodsPriceMessageRequest("CATALOG01", source.barcode) }
                    .channel(catalogMessageChannel.goodsPricingRequestChannel())
                    .get()

    @Bean
    fun createSalesOrderUseCaseAggregator(goodsRepository: GoodsRepository,
                                          catalogMessageChannel: CatalogMessageChannel,
                                          redisMessageStore: RedisMessageStore) =
            IntegrationFlows.from(responseChannelAdapter())
                    .aggregate { aggregatorSpec ->
                        aggregatorSpec
                                .messageStore(redisMessageStore)
                                .outputProcessor(SalesOrderGoodsAggregator())
                    }
                    .route("headers['goods-to-remove']", { t: RouterSpec<String, ExpressionEvaluatingRouter> ->
                        t.channelMapping("false", "processSalesOrderGoods")
                                .channelMapping("true", "rollbackSalesOrderGoods")
                    })
                    .get()

    @Bean
    fun rollbackSalesOrderGoodsPipeline(goodsRepository: GoodsRepository,
                                        catalogMessageChannel: CatalogMessageChannel,
                                        redisMessageStore: RedisMessageStore) =
            IntegrationFlows.from(rollbackSalesOrderGoods())
                    .handle { goods: List<SalesOrderGoods> ->
                        println("rollback goods")
                        println(goods)
                        Unit
                    }.get()


    @Bean
    fun processSalesOrderGoodsPipeline(goodsRepository: GoodsRepository,
                                       catalogMessageChannel: CatalogMessageChannel,
                                       redisMessageStore: RedisMessageStore) =
            IntegrationFlows.from(processSalesOrderGoods())
                    .log()
                    .handle { goods: List<SalesOrderGoods> ->
                        println("processSalesOrderGoods goods")

                        goodsRepository.saveAll(goods)
                                .subscribeOn(Schedulers.elastic())
                                .subscribe()
                        Unit
                    }.get()

}

class CreateSalesOrderListener(private val salesOrderCustomerRepository: SalesOrderCustomerRepository) {

    @StreamListener
    fun execute(@Input("createSalesOrderRequestChannel") input: Flux<Message<NewSalesOrderRequest>>,
                @Output("createSalesOrderResponseChannel") output: FluxSender) {
        output.send(
                input.flatMap { message ->
                    message.payload.let { payload ->
                        salesOrderCustomerRepository.save(CustomerSalesOrder(id = payload.salesOrderId, firstName = payload.customer.firstName, lastName = payload.customer.lastName))
                                .flatMap { salesOrder ->
                                    MessageBuilder
                                            .withPayload(payload.goods.mapIndexed { index, goods -> newGoodsRequest(goods) })
                                            .copyHeaders(mapOf("sales-order-id" to salesOrder.id))
                                            .build()
                                            .toMono()
                                }
                    }
                }
        )
    }

    private fun newGoodsRequest(goods: GoodsRequest): GoodsRequest {
        return GoodsRequest(barcode = goods.barcode, quantity = goods.quantity)
    }

    fun undo(customerSalesOrder: CustomerSalesOrder) = Mono.just(TODO())
}


@Configuration
class NewSalesOrderPipelineConfig {

    @Bean
    fun newSalesOrderRequestChannel() = MessageChannels.flux()

    @Bean
    fun newSalesOrderResponseChannel() = MessageChannels.direct()


    @Bean
    fun newSalesOrderipeline(salesOrderMessageChannel: SalesOrderMessageChannel) =
            IntegrationFlows.from(newSalesOrderRequestChannel())
                    .publishSubscribeChannel { channel ->
                        channel.subscribe { flow ->
                            flow.transform("payload.salesOrderId")
                                    .channel(newSalesOrderResponseChannel())
                        }

                        channel.subscribe { flow ->
                            flow.enrichHeaders(mapOf("execution-id" to UUID.randomUUID().toString()))
                                    .channel(salesOrderMessageChannel.createSalesOrderRequestChannel())
                        }
                    }
                    .get()
}

class SalesOrderGoodsAggregator : DefaultAggregatingMessageGroupProcessor() {

    override fun aggregateHeaders(group: MessageGroup): MutableMap<String, Any> {
        val aggregateHeaders = super.aggregateHeaders(group)
        val aggregation = group.messages.stream().anyMatch { msg: Message<*> -> haveToRollback(msg) }.or(false)
        aggregateHeaders.put("goods-to-remove", aggregation)
        return aggregateHeaders
    }
}

private fun haveToRollback(message: Message<*>) =
        message.headers.getOrDefault("goods-to-remove", false) as Boolean
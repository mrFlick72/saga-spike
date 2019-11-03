package it.valeriovaudi.sagaspike.salesorderservice.messaging

import it.valeriovaudi.sagaspike.salesorderservice.Goods
import it.valeriovaudi.sagaspike.salesorderservice.GoodsRepository
import it.valeriovaudi.sagaspike.salesorderservice.SalesOrderCustomer
import it.valeriovaudi.sagaspike.salesorderservice.SalesOrderCustomerRepository
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.Output
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.reactive.FluxSender
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.integration.annotation.Gateway
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.dsl.EnricherSpec
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.dsl.MessageChannels
import org.springframework.integration.redis.store.RedisMessageStore
import org.springframework.messaging.Message
import org.springframework.messaging.SubscribableChannel
import org.springframework.messaging.handler.annotation.Payload
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.core.scheduler.Schedulers
import java.util.*

data class CreateSalesOrderRequest(var salesOrderId: String? = null, var customer: Customer, var goods: List<GoodsRequest> = emptyList()) {
    constructor() : this(UUID.randomUUID().toString(), Customer("", ""), emptyList())
}
data class Customer(var firstName: String, var lastName: String) {
    constructor() : this("", "")
}

data class GoodsRequest(var salesOrderId: String? = null, var barcode: String, var quantity: Int) {
    constructor() : this(barcode = "", quantity = 0)
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
    fun redisMessageStore(redisConnectionFactory: RedisConnectionFactory) =
            RedisMessageStore(redisConnectionFactory)

    @Bean
    fun createSalesOrderUseCaseSplittator(catalogMessageChannel: CatalogMessageChannel) =
            IntegrationFlows.from("createSalesOrderResponseChannel")
                    .split()
                    .enrich { t: EnricherSpec -> t.headerExpression("goods-quantity", "payload.quantity") }
                    .enrich { t: EnricherSpec -> t.headerExpression("sales-order-id", "payload.salesOrderId") }
                    .transform { source: GoodsRequest -> GoodsPriceMessageRequest("CATALOG01", source.barcode) }
                    .channel(catalogMessageChannel.goodsPricingRequestChannel())
                    .get()

    @Bean
    fun createSalesOrderUseCaseAggregator(goodsRepository: GoodsRepository,
                                          catalogMessageChannel: CatalogMessageChannel,
                                          redisMessageStore: RedisMessageStore) =
            IntegrationFlows.from(responseChannelAdapter())
                    .aggregate { aggregatorSpec -> aggregatorSpec.messageStore(redisMessageStore) }
                    .handle { goods: List<Goods> ->
                        goodsRepository.saveAll(goods)
                                .subscribeOn(Schedulers.elastic())
                                .subscribe()
                        Unit
                    }.get()
}

class CreateSalesOrderListener(private val salesOrderCustomerRepository: SalesOrderCustomerRepository) {

    @StreamListener
    fun execute(@Input("createSalesOrderRequestChannel") input: Flux<Message<CreateSalesOrderRequest>>,
                @Output("createSalesOrderResponseChannel") output: FluxSender) {
        output.send(
                input.flatMap { message ->
                    message.payload.let { payload ->
                        salesOrderCustomerRepository.save(SalesOrderCustomer(id = payload.salesOrderId, firstName = payload.customer.firstName, lastName = payload.customer.lastName))
                                .flatMap { salesOrder ->
                                    payload.goods.mapIndexed { index, goods ->
                                        newGoodsRequest(salesOrder, goods)
                                    }.toMono()
                                }
                    }
                }
        )
    }

    private fun newGoodsRequest(salesOrderCustomer: SalesOrderCustomer, goods: GoodsRequest): GoodsRequest {
        return GoodsRequest(salesOrderId = salesOrderCustomer.id, barcode = goods.barcode, quantity = goods.quantity)
    }

    private fun headers(index: Int, size: Int) = mapOf("message-sequence.index" to index, "message-sequence.sze" to size)

    fun undo(salesOrderCustomer: SalesOrderCustomer) = Mono.just(TODO())
}

@MessagingGateway
interface NewSalesOrderGateway {

    @Gateway(requestChannel = "newSalesOrderRequestChannel", replyChannel = "newSalesOrderResponseChannel")
    fun newSalesOrder(@Payload createSalesOrderRequest: CreateSalesOrderRequest): Mono<String>
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

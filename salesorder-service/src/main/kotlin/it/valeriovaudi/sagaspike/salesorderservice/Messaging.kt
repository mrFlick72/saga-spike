package it.valeriovaudi.sagaspike.salesorderservice

import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.Output
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.reactive.FluxSender
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.integration.dsl.EnricherSpec
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.dsl.MessageChannels
import org.springframework.integration.redis.store.RedisMessageStore
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.SubscribableChannel
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import java.math.BigDecimal
import java.util.*

data class CreateSalesOrderRequest(var salesOrderId: String? = null, var customer: Customer, var goods: List<GoodsRequest> = emptyList()) {
    constructor() : this(UUID.randomUUID().toString(), Customer("", ""), emptyList())
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
    fun responseChannelAdapter() = MessageChannels.direct()

    @Bean
    fun createSalesOrderResponseChannel() = MessageChannels.flux().get()

    @Bean
    fun redisMessageStore(redisConnectionFactory: RedisConnectionFactory) =
            RedisMessageStore(redisConnectionFactory)

    @Bean
    fun createSalesOrderUseCaseSplittator(catalogMessageChannel: CatalogMessageChannel) =
            IntegrationFlows.from("createSalesOrderResponseChannel")
                    .split()
                    .enrich { t: EnricherSpec -> t.headerExpression("goods-quantity", "payload.quantity") }
                    .enrich { t: EnricherSpec -> t.headerExpression("sales-order-id", "payload.salesOrderId") }
                    .transform { source: GoodsRequest -> CatalogGoodsWithPriceMessageRequest("CATALOG01", source.barcode) }
                    .channel(catalogMessageChannel.goodsPricingRequestChannel())
                    .get()

    @Bean
    fun createSalesOrderUseCaseAggregator(goodsRepository: GoodsRepository,
                                          catalogMessageChannel: CatalogMessageChannel,
                                          redisMessageStore: RedisMessageStore) =
            IntegrationFlows.from(responseChannelAdapter())
                    .aggregate { aggregatorSpec -> aggregatorSpec.messageStore(redisMessageStore) }
                    .handle { goods: List<Goods> ->
                        println("aggregation $goods")
                    }.get()
}

class CreateSalesOrderListener(private val salesOrderRepository: SalesOrderRepository) {

    @StreamListener
    fun execute(@Input("createSalesOrderRequestChannel") input: Flux<Message<CreateSalesOrderRequest>>,
                @Output("createSalesOrderResponseChannel") output: FluxSender) {
        output.send(
                input.flatMap { message ->
                    message.payload.let { payload ->
                        salesOrderRepository.save(SalesOrder(id = payload.salesOrderId, customer = payload.customer))
                                .flatMap { salesOrder ->
                                    payload.goods.mapIndexed { index, goods ->
                                        newGoodsRequest(salesOrder, goods)
                                    }.toMono()
                                }
                    }
                }
        )
    }

    private fun newGoodsRequest(salesOrder: SalesOrder, goods: GoodsRequest): GoodsRequest {
        return GoodsRequest(salesOrderId = salesOrder.id, barcode = goods.barcode, quantity = goods.quantity)
    }

    private fun headers(index: Int, size: Int) = mapOf("message-sequence.index" to index, "message-sequence.sze" to size)

    fun undo(salesOrder: SalesOrder) = Mono.just(TODO())
}



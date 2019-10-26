package it.valeriovaudi.sagaspike.salesorderservice

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.Output
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.reactive.FluxSender
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.SubscribableChannel
import org.springframework.messaging.support.MessageBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono

data class CreateSalesOrderRequest(var salesOrderId: String, var customer: Customer, var goods: List<GoodsRequest>)

data class GoodsRequest(var salesOrderId: String? = null, var barcode: String, var quantity: Int) {
    constructor() : this(barcode = "", quantity = 0)
}

interface SalesOrderMessageChannel {

    @Input
    fun createSalesOrderRequestChannel(): SubscribableChannel

    @Output
    fun createSalesOrderResponseChannel(): MessageChannel
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
                                    payload.goods.map { goods ->
                                        MessageBuilder.withPayload(
                                                GoodsRequest(salesOrderId = salesOrder.id, barcode = goods.barcode, quantity = goods.quantity)
                                        ).build()
                                    }.toMono()
                                }
                    }
                }.flatMap { Flux.fromIterable(it) }
        )
    }

    fun undo(salesOrder: SalesOrder) = Mono.just(TODO())
}



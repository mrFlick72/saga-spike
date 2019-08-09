package it.valeriovaudi.sagaspike.salesorderservice

import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.Output
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.reactive.FluxSender
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.SubscribableChannel
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

data class CreateSalesOrderRequest(var customer: Customer, var goods: List<GoodsRequest>)
data class GoodsRequest(var salesOrderId: String, var barcode: String, var quantity: Int)

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
        output.send(input.flatMap { message ->
            message.payload.let {
                salesOrderRepository.save(SalesOrder(customer = it.customer))
            }
        })

    }

    fun undo(salesOrder: SalesOrder) = Mono.just(TODO())
}



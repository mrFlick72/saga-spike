package it.valeriovaudi.sagaspike.salesorderservice.messaging

import it.valeriovaudi.sagaspike.salesorderservice.Money
import it.valeriovaudi.sagaspike.salesorderservice.SalesOrderGoods
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.Output
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.reactive.FluxSender
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.SubscribableChannel
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.toMono
import java.io.Serializable
import java.math.BigDecimal
import java.util.*

data class ReserveGoodsMessage(var barcode: String, var quantity: Int) : Serializable {
    constructor() : this("", 0)
}

interface InventoryMessageChannel {

    @Output
    fun reserveGoodsRequestChannel(): MessageChannel

    @Input
    fun reserveGoodsResponseChannel(): SubscribableChannel

}

@Component
class InventoryMessagingListeners {

    @StreamListener
    fun execute(@Input("reserveGoodsResponseChannel") input: Flux<Message<ReserveGoodsMessage>>,
                @Output("responseChannelAdapter") output: FluxSender) {
        output.send(
                input.flatMap { message ->
                    println("reserveGoodsStreamListener $message");

                    val goods = SalesOrderGoods(id = UUID.randomUUID().toString(),
                            salesOrderId = message.headers["sales-order-id"] as String,
                            quantity = message.payload.quantity,
                            barcode = message.payload.barcode,
                            name = message.headers["goods-name"] as String,
                            price = Money(BigDecimal(message.headers["goods-price"] as String), message.headers["currency"] as String))


                    MessageBuilder.withPayload(goods)
                            .copyHeaders(MessageUtils.copyHeaders(message.headers))
                            .build().toMono()
                }
        )
    }

}
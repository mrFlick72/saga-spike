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

open class GoodsMessage(var barcode: String, var quantity: Int) : Serializable {
    constructor() : this("", 0)
}

class NotAvailableGoods(barcode: String, quantity: Int, var errorMessage: String) : GoodsMessage(barcode, quantity) {
    constructor() : this("", 0, "")
}

class ReserveGoodsMessage(barcode: String, quantity: Int) : GoodsMessage(barcode, quantity) {
    constructor() : this("", 0)
}

data class SalesOrderGoodsMessageWrapper(var salesOrderGoods: SalesOrderGoods, var hasRollback: Boolean) : Serializable

interface InventoryMessageChannel {

    @Output
    fun reserveGoodsRequestChannel(): MessageChannel

    @Input
    fun reserveGoodsResponseChannel(): SubscribableChannel

    @Input
    fun reserveGoodsErrorChannel(): SubscribableChannel

    @Output
    fun unReserveGoodsRequestChannel(): MessageChannel
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


                    MessageBuilder.withPayload(SalesOrderGoodsMessageWrapper(goods, false))
                            .copyHeaders(MessageUtils.copyHeaders(message.headers))
                            .setHeader("goods-to-remove", false)
                            .build().toMono()
                }
        )
    }


    @StreamListener
    fun errorHandling(@Input("reserveGoodsErrorChannel") input: Flux<Message<NotAvailableGoods>>,
                      @Output("responseChannelAdapter") output: FluxSender) {
        output.send(
                input.flatMap { message ->
                    println("errorHandlingStreamListener $message");

                    val goods = SalesOrderGoods(id = UUID.randomUUID().toString(),
                            salesOrderId = message.headers.getOrDefault("sales-order-id", "") as String,
                            quantity = message.payload.quantity,
                            barcode = message.payload.barcode,
                            name = message.headers.getOrDefault("goods-name", "") as String,
                            price = Money(
                                    BigDecimal(message.headers.getOrDefault("goods-price", "0") as String),
                                    message.headers.getOrDefault("currency", "") as String
                            )
                    )


                    MessageBuilder.withPayload(SalesOrderGoodsMessageWrapper(goods, true))
                            .copyHeaders(MessageUtils.copyHeaders(message.headers))
                            .setHeader("goods-to-remove", true)
                            .build().toMono()
                }
        )

    }

}
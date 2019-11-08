package it.valeriovaudi.sagaspike.salesorderservice.messaging

import it.valeriovaudi.sagaspike.salesorderservice.messaging.MessageUtils.copyHeaders
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

data class GoodsPriceMessageRequest(val catalogId: String, val barcode: String) : Serializable

data class GoodsPriceMessageResponse(var goods: CatalogGoods, var price: CatalogPrice) : Serializable {
    constructor() : this(CatalogGoods(), CatalogPrice())
}

data class CatalogGoods(var barcode: String, var name: String) : Serializable {
    constructor() : this("", "")
}

data class CatalogPrice(var price: BigDecimal, var currency: String) : Serializable {
    constructor() : this(BigDecimal.ZERO, "")
}


interface CatalogMessageChannel {

    @Output
    fun goodsPricingRequestChannel(): MessageChannel

    @Input
    fun goodsPricingResponseChannel(): SubscribableChannel
}

@Component
class CatalogMessagingListeners {

    @StreamListener
    fun goodsPricingStreamListener(@Input("goodsPricingResponseChannel") input: Flux<Message<GoodsPriceMessageResponse>>,
                                   @Output("reserveGoodsRequestChannel") output: FluxSender) {
        output.send(
                input.flatMap { message ->
                    val payload = ReserveGoodsMessage(message.payload.goods.barcode, message.headers["goods-quantity"] as Int)
                    println("goodsPricingStreamListener $payload");

                    MessageBuilder.withPayload(payload)
                            .copyHeaders(mapOf(
                                    "goods-name" to message.payload.goods.name,
                                    "goods-price" to message.payload.price.price.toString(),
                                    "currency" to message.payload.price.currency)
                            )
                            .copyHeaders(copyHeaders(message.headers))
                            .build()
                            .toMono()
                })
    }
}
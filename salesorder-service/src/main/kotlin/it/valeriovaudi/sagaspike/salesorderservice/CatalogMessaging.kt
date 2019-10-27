package it.valeriovaudi.sagaspike.salesorderservice

import it.valeriovaudi.sagaspike.salesorderservice.MessageUtils.copyHeaders
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
import java.math.BigDecimal


data class CatalogGoodsWithPriceMessageRequest(val catalogId: String, val barcode: String)

interface CatalogMessageChannel {

    @Output
    fun goodsPricingRequestChannel(): MessageChannel

    @Input
    fun goodsPricingResponseChannel(): SubscribableChannel
}

@Component
class CatalogMessagingListeners {
    @StreamListener
    fun goodsPricingResponseChannelAdapter(@Input("goodsPricingResponseChannel") input: Flux<Message<CatalogGoodsWithPrice>>,
                                           @Output("goodsPricingResponseChannelAdapter") output: FluxSender) {
        output.send(input.flatMap { message ->
            println("goodsPricingResponseChannelAdapter $message");
            val headers = message.headers
            MessageBuilder.withPayload(CatalogGoodsWithPrice(message.payload.goods, message.payload.price))
                    .copyHeaders(copyHeaders(message.headers))
                    .build()
                    .toMono()
        })
    }
}

data class CatalogGoods(var barcode: String, var name: String)

data class CatalogPrice(var price: BigDecimal, var currency: String)

data class CatalogGoodsWithPrice(var goods: CatalogGoods, var price: CatalogPrice)

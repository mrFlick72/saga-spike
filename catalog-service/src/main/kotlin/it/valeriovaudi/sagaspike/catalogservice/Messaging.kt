package it.valeriovaudi.sagaspike.catalogservice

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


interface CatalogMessageChannel {

    @Input
    fun goodsPricingRequestChannel(): SubscribableChannel

    @Output
    fun goodsPricingResponseChannel(): MessageChannel
}


class GetPriceListener(private val findGoodsInCatalog: FindGoodsInCatalog) {

//    blocking version

    /*@StreamListener("goodsPricingRequestChannel")
    @SendTo("goodsPricingResponseChannel")
    fun handleGoodsPriceRequest(message: Message<GoodsWithPriceMessageRequest>): Message<GoodsWithPrice>? {

        println("handleGoodsPriceRequest invoked")
        println("message $message")

        return catalogRepository.findAll()
                .log()
                .filter { it.id == message.payload.catalogId }
                .flatMapIterable { it.goods }
                .filter { it.goods.barcode == message.payload.barcode }
                .map { withPayload(it).build() }
                .toMono()
                .block()
    }*/

    @StreamListener
    fun handleGoodsPriceRequest(@Input("goodsPricingRequestChannel") input: Flux<Message<GoodsWithPriceMessageRequest>>,
                                @Output("goodsPricingResponseChannel") output: FluxSender) {

        output.send(
                input.flatMap { message ->
                    message.payload.let { findGoodsInCatalog.findFor(it.catalogId, it.barcode) }
                            .map<Message<GoodsWithPrice>> { goodsWithPrice -> MessageBuilder.withPayload(goodsWithPrice).build() }

                })
    }

}

data class GoodsWithPriceMessageRequest(val catalogId: String, val barcode: String)

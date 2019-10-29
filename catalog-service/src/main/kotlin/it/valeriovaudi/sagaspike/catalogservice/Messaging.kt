package it.valeriovaudi.sagaspike.catalogservice

import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.Output
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.reactive.FluxSender
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.SubscribableChannel
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageBuilder
import reactor.core.publisher.Flux

interface CatalogMessageChannel {

    @Input
    fun goodsPricingRequestChannel(): SubscribableChannel

    @Output
    fun goodsPricingResponseChannel(): MessageChannel
}


class GetPriceListener(private val findGoodsInCatalog: FindGoodsInCatalog) {

     @StreamListener
     fun handleGoodsPriceRequest(@Input("goodsPricingRequestChannel") input: Flux<Message<GoodsWithPriceMessageRequest>>,
                                 @Output("goodsPricingResponseChannel") output: FluxSender) {

         output.send(
                 input.flatMap { message ->
                     println(message)
                     message.payload.let { findGoodsInCatalog.findFor(it.catalogId, it.barcode) }
                             .map<Message<GoodsWithPrice>> { goodsWithPrice ->
                                 println("send a message")
                                 MessageBuilder.withPayload(goodsWithPrice)
                                         .copyHeaders(MessageUtils.copyHeaders(message.headers))
                                         .build()
                             }

                 })
     }
}

data class GoodsWithPriceMessageRequest(val catalogId: String, val barcode: String) {
    constructor() : this("", "")
}

class ExecutionIdPropagatorChannelInterceptor : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        return MessageBuilder.fromMessage(message)
                .setHeaderIfAbsent("execution-id", message.headers.getOrDefault("execution-id", ""))
                .build()
    }
}
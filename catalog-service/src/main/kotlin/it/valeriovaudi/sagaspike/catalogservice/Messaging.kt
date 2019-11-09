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
import reactor.core.publisher.Mono

interface CatalogMessageChannel {

    @Input
    fun goodsPricingRequestChannel(): SubscribableChannel

    @Output
    fun goodsPricingResponseChannel(): MessageChannel

    @Output
    fun goodsPricingErrorChannel(): MessageChannel
}


class GetPriceListener(private val findGoodsInCatalog: FindGoodsInCatalog) {

    @StreamListener
    fun handleGoodsPriceRequest(@Input("goodsPricingRequestChannel") input: Flux<Message<GoodsWithPriceMessageRequest>>,
                                @Output("goodsPricingResponseChannel") output: FluxSender,
                                @Output("goodsPricingErrorChannel") error: FluxSender) {

        output.send(
                input.flatMap { message ->
                    println(message)
                    message.payload.let { payload ->
                        findGoodsInCatalog.findFor(payload.catalogId, payload.barcode)
                                .map<Message<GoodsWithPrice>> { goodsWithPrice ->
                                    println("send a message")
                                    MessageBuilder.withPayload(goodsWithPrice)
                                            .copyHeaders(MessageUtils.copyHeaders(message.headers))
                                            .build()
                                }
                                .onErrorResume(sendErrorMessage(error, message))

                    }

                })
    }

    private fun sendErrorMessage(error: FluxSender, message: Message<GoodsWithPriceMessageRequest>): (Throwable) -> Mono<Message<GoodsWithPrice>> {
        return { e ->
            error.send(
                    message.payload.let {
                        println("sendErrorMessage")
                        Flux.just(MessageBuilder.withPayload(GoodsNotInCatalogMessage(it.barcode))
                                .copyHeaders(MessageUtils.copyHeaders(message.headers))
                                .setHeader("goods-not-in-catalog", true)
                                .build())
                    }
            ).then(Mono.empty())
        }
    }
}

data class GoodsWithPriceMessageRequest(val catalogId: String, val barcode: String) {
    constructor() : this("", "")
}

data class GoodsNotInCatalogMessage(val barcode: String) {
    constructor() : this("")
}

class ExecutionIdPropagatorChannelInterceptor : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        return MessageBuilder.fromMessage(message)
                .setHeaderIfAbsent("execution-id", message.headers.getOrDefault("execution-id", ""))
                .build()
    }
}
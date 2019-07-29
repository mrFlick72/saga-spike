package it.valeriovaudi.sagaspike.inventoryservice

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


interface InventoryMessageChannel {

    @Input
    fun reserveGoodsRequestChannel(): SubscribableChannel

    @Output
    fun reserveGoodsResponseChannel(): MessageChannel


    @Input
    fun unReserveGoodsRequestChannel(): SubscribableChannel

    @Output
    fun unReserveGoodsResponseChannel(): MessageChannel

}

class ReserveGoodsListener(private val reserveGoods: ReserveGoods) {

    @StreamListener(copyHeaders = "execution-id")
    fun handleGoodsReservation(@Input("reserveGoodsRequestChannel") input: Flux<Message<ReserveGoodsQuantity>>,
                               @Output("reserveGoodsResponseChannel") output: FluxSender,
                               @Output("reserveGoodsRequestChannel.reserveGoodsRequest.errors") error: FluxSender) {
        output.send(input.flatMap { message ->
            message.payload.let { reserveGoods.execute(it.barcode, it.quantity) }
                    .map { MessageBuilder.withPayload(ReservedGoodsQuantity(message.payload.barcode, message.payload.quantity)).build() }
                    .onErrorResume { e -> error.send(Flux.just(e)).then(Mono.empty()) }

        })
    }

    @StreamListener
    fun handleGoodsUnReservation(@Input("unReserveGoodsRequestChannel") input: Flux<Message<ReserveGoodsQuantity>>,
                                 @Output("unReserveGoodsResponseChannel") output: FluxSender,
                                 @Output("unReserveGoodsResponseChannel.reserveGoodsRequest.errors") error: FluxSender) {
        output.send(input.flatMap { message ->
            message.payload.let { reserveGoods.undo(it.barcode, it.quantity) }
                    .map { MessageBuilder.withPayload(ReservedGoodsQuantity(message.payload.barcode, message.payload.quantity)).build() }
                    .onErrorResume { e -> error.send(Flux.just(e)).then(Mono.empty()) }

        })
    }
}


data class ReserveGoodsQuantity(var barcode: String, var quantity: Int) {
    constructor() : this("", 0)
}

data class ReservedGoodsQuantity(var barcode: String, var quantity: Int) {
    constructor() : this("", 0)
}
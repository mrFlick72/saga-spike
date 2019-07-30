package it.valeriovaudi.sagaspike.inventoryservice

import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.Output
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.reactive.FluxSender
import org.springframework.context.annotation.Configuration
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.SubscribableChannel
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
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

    @StreamListener
    fun handleGoodsReservation(@Input("reserveGoodsRequestChannel") input: Flux<Message<ReserveGoodsQuantity>>,
                               @Output("reserveGoodsResponseChannel") output: FluxSender,
                               @Output("reserveGoodsRequestChannel.reserveGoodsRequest.errors") error: FluxSender) {
        output.send(input.flatMap { message ->
            message.payload.let {
                reserveGoods.execute(it.barcode, it.quantity)
            }
                    .map(sendSuccessfulMessage(message))
                    .onErrorResume(sendErrorMessage(error, message))

        })
    }


    @StreamListener
    fun handleGoodsUnReservation(@Input("unReserveGoodsRequestChannel") input: Flux<Message<ReserveGoodsQuantity>>,
                                 @Output("unReserveGoodsResponseChannel") output: FluxSender) {
        output.send(input.flatMap { message ->
            message.payload.let {
                reserveGoods.undo(it.barcode, it.quantity)
            }
                    .map(sendSuccessfulMessage(message))
        })
    }

    private fun sendSuccessfulMessage(message: Message<ReserveGoodsQuantity>): (Goods) -> Message<ReservedGoodsQuantity> {
        return {
            MessageBuilder.withPayload(ReservedGoodsQuantity(message.payload.barcode, message.payload.quantity))
                    .setHeaderIfAbsent("execution-id", message.headers.getOrDefault("execution-id", ""))
                    .build()
        }
    }

    private fun sendErrorMessage(error: FluxSender,
                                 message: Message<ReserveGoodsQuantity>): (Throwable) -> Mono<Message<ReservedGoodsQuantity>> {
        return { e ->
            error.send(Flux.just(MessageBuilder.withPayload(e)
                    .setHeaderIfAbsent("execution-id", message.headers.getOrDefault("execution-id", ""))
                    .build()))
                    .then(Mono.empty())
        }
    }
}

data class ReserveGoodsQuantity(var barcode: String, var quantity: Int) {
    constructor() : this("", 0)
}

data class ReservedGoodsQuantity(var barcode: String, var quantity: Int) {
    constructor() : this("", 0)
}

@Configuration
class ErrorHandling(private val errorLogger: ErrorLogger) {

    @ServiceActivator(inputChannel = "reserveGoodsRequestChannel.reserveGoodsRequest.errors")
    fun error(message: Message<*>) {
        println(errorLogger)
        errorLogger.log(message)
    }

}

@Component
class ErrorLogger {

    fun log(message: Message<*>) {
        println("Handling ERROR: $message")
    }
}
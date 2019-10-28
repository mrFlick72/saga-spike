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
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


interface InventoryMessageChannel {

    @Input
    fun reserveGoodsRequestChannel(): SubscribableChannel

    @Output
    fun reserveGoodsResponseChannel(): MessageChannel

    @Output
    fun reserveGoodsErrorChannel(): MessageChannel

    @Input
    fun unReserveGoodsRequestChannel(): SubscribableChannel

    @Output
    fun unReserveGoodsResponseChannel(): MessageChannel

}

class ReserveGoodsListener(private val reserveGoods: ReserveGoods) {

    @StreamListener
    fun handleGoodsReservation(@Input("reserveGoodsRequestChannel") input: Flux<Message<ReserveGoodsQuantity>>,
                               @Output("reserveGoodsResponseChannel") output: FluxSender,
                               @Output("reserveGoodsErrorChannel") error: FluxSender) {
        output.send(input.flatMap { message ->
            message.payload.let {
                reserveGoods.execute(it.barcode, it.quantity)
                        .map(sendSuccessfulMessage(message))
                        .onErrorResume(sendErrorMessage(error, message))
            }
        })
    }


    @StreamListener
    fun handleGoodsUnReservation(@Input("unReserveGoodsRequestChannel") input: Flux<Message<ReserveGoodsQuantity>>,
                                 @Output("unReserveGoodsResponseChannel") output: FluxSender) {
        output.send(input.flatMap { message ->
            message.payload.let {
                reserveGoods.undo(it.barcode, it.quantity)
                        .map(sendSuccessfulMessage(message))
            }
        })
    }

    private fun sendSuccessfulMessage(message: Message<ReserveGoodsQuantity>): (Goods) -> Message<ReservedGoodsQuantity> {
        return {
            MessageBuilder.withPayload(ReservedGoodsQuantity(message.payload.barcode, message.payload.quantity))
                    .copyHeaders(MessageUtils.copyHeaders(message.headers))
                    .build()
        }
    }

    private fun sendErrorMessage(error: FluxSender, message: Message<ReserveGoodsQuantity>): (Throwable) -> Mono<Message<ReservedGoodsQuantity>> {
        return { e ->
            error.send(
                    message.payload.let {
                        Flux.just(NotAvailableGoods(
                                barcode = it.barcode,
                                quantity = it.quantity,
                                errorMessage = e.message ?: ""))
                    }
            )
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
        errorLogger.log(message)
    }

}

open class ErrorLogger {

    open fun log(message: Message<*>?) {
        println("Handling ERROR: $message")
    }
}


class ExecutionIdPropagatorChannelInterceptor : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        return MessageBuilder.fromMessage(message)
                .setHeaderIfAbsent("execution-id", message.headers.getOrDefault("execution-id", ""))
                .build()
    }
}
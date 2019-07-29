package it.valeriovaudi.sagaspike.messageemitter

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.Output
import org.springframework.integration.dsl.MessageChannels
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.SubscribableChannel
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import java.util.*

@SpringBootApplication
@EnableBinding(InventoryMessageChannel::class)
class MessageEmitterApplication

fun main(args: Array<String>) {
    runApplication<MessageEmitterApplication>(*args)
}


@Component
class MessageSender(private val inventoryMessageChannel: InventoryMessageChannel) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        inventoryMessageChannel.reserveGoodsRequestChannel()
                .send(MessageBuilder.withPayload(ReserveGoodsQuantity("A_BARCODE_1", 15))
                        .setErrorChannel(MessageChannels.publishSubscribe("reserveGoodsRequestChannel.reserveGoodsRequest.errors").get())
                        .setHeaderIfAbsent("execution-id", UUID.randomUUID().toString()).build())
    }

}


data class ReserveGoodsQuantity(var barcode: String, var quantity: Int) {
    constructor() : this("", 0)
}


interface InventoryMessageChannel {

    @Output
    fun reserveGoodsRequestChannel(): SubscribableChannel

    @Input
    fun reserveGoodsResponseChannel(): MessageChannel


}

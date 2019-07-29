package it.valeriovaudi.sagaspike.inventoryservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.context.annotation.Bean
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.config.EnableIntegration
import org.springframework.messaging.Message


@EnableIntegration
@SpringBootApplication
@EnableBinding(InventoryMessageChannel::class)
class InventoryServiceApplication {

    @Bean
    fun reserveGoods(inventoryRepository: InventoryRepository) = ReserveGoods(inventoryRepository)

    @Bean
    fun reserveGoodsListener(reserveGoods: ReserveGoods) = ReserveGoodsListener(reserveGoods)

    @ServiceActivator(inputChannel = "reserveGoodsRequestChannel.reserveGoodsRequest.errors") //channel name 'input.myGroup.errors'
    fun error(message: Message<*>) {
        println("Handling ERROR: $message")
    }

}

fun main(args: Array<String>) {
    runApplication<InventoryServiceApplication>(*args)
}


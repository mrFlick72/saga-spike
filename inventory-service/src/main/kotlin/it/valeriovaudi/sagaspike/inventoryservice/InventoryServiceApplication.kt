package it.valeriovaudi.sagaspike.inventoryservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.context.annotation.Bean
import org.springframework.integration.config.EnableIntegration


@EnableIntegration
@SpringBootApplication
@EnableBinding(InventoryMessageChannel::class)
class InventoryServiceApplication {

    @Bean
    fun reserveGoods(inventoryRepository: InventoryRepository) = ReserveGoods(inventoryRepository)

    @Bean
    fun reserveGoodsListener(reserveGoods: ReserveGoods) = ReserveGoodsListener(reserveGoods)

    @Bean
    fun errorLogger() = ErrorLogger()
}

fun main(args: Array<String>) {
    runApplication<InventoryServiceApplication>(*args)
}


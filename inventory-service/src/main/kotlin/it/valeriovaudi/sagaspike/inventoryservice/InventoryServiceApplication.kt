package it.valeriovaudi.sagaspike.inventoryservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.context.annotation.Bean
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.config.GlobalChannelInterceptor


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

    @Bean
    @GlobalChannelInterceptor
    fun executionIdPropagatorChannelInterceptor() = ExecutionIdPropagatorChannelInterceptor()
}

fun main(args: Array<String>) {
    runApplication<InventoryServiceApplication>(*args)
}


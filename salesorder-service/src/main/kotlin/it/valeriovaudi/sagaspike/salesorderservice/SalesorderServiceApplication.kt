package it.valeriovaudi.sagaspike.salesorderservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.context.annotation.Bean
import org.springframework.integration.annotation.IntegrationComponentScan
import org.springframework.integration.config.EnableIntegration

@EnableIntegration
@SpringBootApplication
@EnableBinding(SalesOrderMessageChannel::class, CatalogMessageChannel::class, InventoryMessageChannel::class)
class SalesorderServiceApplication {

    @Bean
    fun createSalesOrderListener(salesOrderRepository: SalesOrderRepository) = CreateSalesOrderListener(salesOrderRepository)
}

fun main(args: Array<String>) {
    runApplication<SalesorderServiceApplication>(*args)
}

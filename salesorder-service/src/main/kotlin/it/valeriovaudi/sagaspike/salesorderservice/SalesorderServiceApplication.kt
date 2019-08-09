package it.valeriovaudi.sagaspike.salesorderservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.context.annotation.Bean

@SpringBootApplication
@EnableBinding(SalesOrderMessageChannel::class)
class SalesorderServiceApplication {

    @Bean
    fun createSalesOrderListener(salesOrderRepository: SalesOrderRepository) = CreateSalesOrderListener(salesOrderRepository)
}

fun main(args: Array<String>) {
    runApplication<SalesorderServiceApplication>(*args)
}

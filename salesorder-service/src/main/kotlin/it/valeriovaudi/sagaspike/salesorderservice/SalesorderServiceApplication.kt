package it.valeriovaudi.sagaspike.salesorderservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.context.annotation.Bean
import org.springframework.integration.annotation.IntegrationComponentScan
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.config.GlobalChannelInterceptor
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageBuilder
import java.util.*

@EnableIntegration
@SpringBootApplication
@EnableBinding(SalesOrderMessageChannel::class, CatalogMessageChannel::class, InventoryMessageChannel::class)
class SalesorderServiceApplication {

    @Bean
    fun createSalesOrderListener(salesOrderRepository: SalesOrderRepository) = CreateSalesOrderListener(salesOrderRepository)

    @Bean
    @GlobalChannelInterceptor
    fun executionIdPropagatorChannelInterceptor() = ExecutionIdPropagatorChannelInterceptor()
}

fun main(args: Array<String>) {
    runApplication<SalesorderServiceApplication>(*args)
}
class ExecutionIdPropagatorChannelInterceptor : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        return MessageBuilder.fromMessage(message)
                .setHeaderIfAbsent("execution-id", message.headers.getOrDefault("execution-id", UUID.randomUUID().toString()))
                .build()
    }
}
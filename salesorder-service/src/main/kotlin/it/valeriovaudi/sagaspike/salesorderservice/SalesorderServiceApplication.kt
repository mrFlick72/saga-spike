package it.valeriovaudi.sagaspike.salesorderservice

import it.valeriovaudi.sagaspike.salesorderservice.messaging.CatalogMessageChannel
import it.valeriovaudi.sagaspike.salesorderservice.messaging.InventoryMessageChannel
import it.valeriovaudi.sagaspike.salesorderservice.messaging.salesorder.CreateSalesOrderListener
import it.valeriovaudi.sagaspike.salesorderservice.messaging.salesorder.SalesOrderMessageChannel
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.integration.annotation.IntegrationComponentScan
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.config.GlobalChannelInterceptor
import org.springframework.integration.redis.store.RedisMessageStore
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageBuilder
import java.util.*

@EnableIntegration
@SpringBootApplication
@IntegrationComponentScan
@EnableBinding(SalesOrderMessageChannel::class, CatalogMessageChannel::class, InventoryMessageChannel::class)
class SalesorderServiceApplication {

    @Bean
    fun createSalesOrderListener(salesOrderCustomerRepository: SalesOrderCustomerRepository) = CreateSalesOrderListener(salesOrderCustomerRepository)

    @Bean
    @GlobalChannelInterceptor
    fun executionIdPropagatorChannelInterceptor() = ExecutionIdPropagatorChannelInterceptor()

    @Bean
    fun redisMessageStore(redisConnectionFactory: RedisConnectionFactory) =
            RedisMessageStore(redisConnectionFactory)

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
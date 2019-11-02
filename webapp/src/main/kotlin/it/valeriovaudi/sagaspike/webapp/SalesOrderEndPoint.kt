package it.valeriovaudi.sagaspike.webapp

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.annotation.Gateway
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.dsl.MessageChannels
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import java.util.*

@Configuration
class SalesOrderEndPoint(private val newSalesOrderGateway: NewSalesOrderGateway) {

    @Bean
    fun routes() = router {
        POST("/sales-order") {
            it.bodyToMono(CreateSalesOrderRequest::class.java)
                    .flatMap {newSalesOrderGateway.newSalesOrder(it) }
                    .flatMap { ok().body(BodyInserters.fromObject(it)) }
        }
    }
}

@MessagingGateway
interface NewSalesOrderGateway {

    @Gateway(requestChannel = "newSalesOrderRequestChannel", replyChannel = "newSalesOrderResponseChannel")
    fun newSalesOrder(@Payload createSalesOrderRequest: CreateSalesOrderRequest): Mono<String>
}

@Configuration
class NewSalesOrderPipelineConfig {

    @Bean
    fun newSalesOrderRequestChannel() = MessageChannels.flux()

    @Bean
    fun newSalesOrderResponseChannel() = MessageChannels.direct()


    @Bean
    fun newSalesOrderipeline(salesOrderMessageChannel: SalesOrderMessageChannel) =
            IntegrationFlows.from(newSalesOrderRequestChannel())
                    .publishSubscribeChannel { channel ->
                        channel.subscribe { flow ->
                            flow.transform("payload.salesOrderId")
                                    .channel(newSalesOrderResponseChannel())
                        }

                        channel.subscribe { flow ->
                            flow.enrichHeaders(mapOf("execution-id" to UUID.randomUUID().toString()))
                                    .channel(salesOrderMessageChannel.createSalesOrderRequestChannel())
                        }
                    }
                    .get()
}
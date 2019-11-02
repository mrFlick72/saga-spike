package it.valeriovaudi.sagaspike.webapp

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.annotation.Gateway
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.dsl.MessageChannels
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.*

@RestController
class SalesOrderEndPoint(private val newSalesOrderGateway: NewSalesOrderGateway) {

    @PostMapping("/sales-order")
    fun createSaleOrder(@RequestBody createSalesOrderRequest: CreateSalesOrderRequest) =
            Mono.create<String> { sink -> sink.success(newSalesOrderGateway.newSalesOrder(createSalesOrderRequest)) }
}

@MessagingGateway
interface NewSalesOrderGateway {

    @Gateway(requestChannel = "newSalesOrderRequestChannel", replyChannel = "newSalesOrderResponseChannel")
    fun newSalesOrder(@Payload createSalesOrderRequest: CreateSalesOrderRequest): String
}

@Configuration
class NewSalesOrderPipelineConfig {

    @Bean
    fun newSalesOrderRequestChannel() = MessageChannels.direct()

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
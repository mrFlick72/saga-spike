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
import java.util.*

@RestController
class SalesOrderEndPoint(private val newSalesOrderGateway: NewSalesOrderGateway) {

    @PostMapping("/sales-order")
    fun createSaleOrder(@RequestBody createSalesOrderRequest: CreateSalesOrderRequest) =
            newSalesOrderGateway.newSalesOrder(createSalesOrderRequest)
}

@MessagingGateway
interface NewSalesOrderGateway {

    @Gateway(requestChannel = "newSalesOrderRequestChannel", replyChannel = "newSalesOrderResponseChannel")
    fun newSalesOrder(@Payload createSalesOrderRequest: CreateSalesOrderRequest): String
}

@Configuration
class NewSalesOrderPipelineConfig {

    @Bean
    fun newSalesOrderRequestChannel() = MessageChannels.publishSubscribe()

    @Bean
    fun newSalesOrderResponseChannel() = MessageChannels.direct()

    @Bean
    fun newSalesOrderErrorChannel() = MessageChannels.direct()


    @Bean
    fun newSalesOrderIdEchoPipeline(salesOrderMessageChannel: SalesOrderMessageChannel) =
            IntegrationFlows.from(newSalesOrderRequestChannel())
                    .transform { source: CreateSalesOrderRequest -> source.salesOrderId }
                    .channel(newSalesOrderResponseChannel())
                    .get()

    @Bean
    fun newSalesOrderSagaProcessingPipeline(salesOrderMessageChannel: SalesOrderMessageChannel) =
            IntegrationFlows.from(newSalesOrderRequestChannel())
                    .enrichHeaders(mapOf("execution-id" to UUID.randomUUID().toString()))
                    .channel(salesOrderMessageChannel.createSalesOrderRequestChannel())
                    .get()

}
package it.valeriovaudi.sagaspike.webapp

import org.springframework.messaging.support.MessageBuilder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.*

@RestController
class SalesOrderEndPoint(private val salesOrderMessageChannel: SalesOrderMessageChannel) {

    @PostMapping("/sales-order")
    fun createSaleOrder(@RequestBody createSalesOrderRequest: CreateSalesOrderRequest): Mono<String> {
        val executionId = UUID.randomUUID().toString()
        val build = MessageBuilder.withPayload(createSalesOrderRequest).setHeader("execution-id", executionId).build()
        salesOrderMessageChannel.createSalesOrderRequestChannel().send(build)

        return Mono.just(executionId)
    }
}
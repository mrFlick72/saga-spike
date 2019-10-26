package it.valeriovaudi.sagaspike.salesorderservice

import org.springframework.messaging.support.MessageBuilder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SalesOrderEndPoint(private val salesOrderMessageChannel: SalesOrderMessageChannel) {

    @PostMapping("/sales-order")
    fun createSaleOrder(@RequestBody createSalesOrderRequest: CreateSalesOrderRequest){
        val build = MessageBuilder.withPayload(createSalesOrderRequest).build()
        salesOrderMessageChannel.createSalesOrderRequestChannel().send(build)
    }
}
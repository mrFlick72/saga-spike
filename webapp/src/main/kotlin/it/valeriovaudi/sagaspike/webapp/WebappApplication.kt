package it.valeriovaudi.sagaspike.webapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Output
import org.springframework.integration.annotation.IntegrationComponentScan
import org.springframework.integration.config.EnableIntegration
import org.springframework.messaging.SubscribableChannel
import java.util.*

@EnableIntegration
@IntegrationComponentScan
@SpringBootApplication
@EnableBinding(SalesOrderMessageChannel::class)
class WebappApplication

fun main(args: Array<String>) {
    runApplication<WebappApplication>(*args)
}

interface SalesOrderMessageChannel {

    @Output
    fun createSalesOrderRequestChannel(): SubscribableChannel

}


data class GoodsRequest(var salesOrderId: String? = null, var barcode: String, var quantity: Int) {
    constructor() : this(barcode = "", quantity = 0)
}


data class Customer(var firstName: String, var lastName: String) {
    constructor() : this("", "")
}


data class CreateSalesOrderRequest(var salesOrderId: String? = null, var customer: Customer, var goods: List<GoodsRequest> = emptyList()) {
    constructor() : this(UUID.randomUUID().toString(), Customer("", ""), emptyList())
}
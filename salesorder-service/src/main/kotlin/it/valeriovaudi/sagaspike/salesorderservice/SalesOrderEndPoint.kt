package it.valeriovaudi.sagaspike.salesorderservice

import it.valeriovaudi.sagaspike.salesorderservice.messaging.NewSalesOrderGateway
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.router

@Configuration
class SalesOrderEndPoint {

    @Bean
    fun routes(newSalesOrderGateway: NewSalesOrderGateway,
               getSalesOrder: GetSalesOrder) = router {
        POST("/sales-order") {
            it.bodyToMono(it.valeriovaudi.sagaspike.salesorderservice.messaging.CreateSalesOrderRequest::class.java)
                    .flatMap { newSalesOrderGateway.newSalesOrder(it) }
                    .flatMap { ok().body(BodyInserters.fromObject(it)) }
        }

        GET("/sales-order/{salesOrderId}") {
            getSalesOrder.execute(it.pathVariable("salesOrderId"))
                    .flatMap { ok().body(BodyInserters.fromObject(SalesOrderRepresentation.of(it))) }
        }
    }
}


data class SalesOrderRepresentation(val salesOrderId: String, val customer: CustomerRepresentation, val goods: List<GoodsRepresentation>, val total: Money) {
    companion object {
        fun of(salesOrder: SalesOrder) =
                SalesOrderRepresentation(
                        salesOrderId = salesOrder.id,
                        customer = CustomerRepresentation.of(salesOrder.customer),
                        goods = salesOrder.goods.map { GoodsRepresentation.of(it) },
                        total = salesOrder.total)

    }
}

data class GoodsRepresentation(var barcode: String, var name: String, var quantity: Int, var price: Money) {
    companion object {
        fun of(goods: Goods) =
                GoodsRepresentation(goods.barcode, goods.name, goods.quantity, goods.price)
    }
}

data class CustomerRepresentation(val firstName: String, val lastName: String) {
    companion object {
        fun of(customer: SalesOrderCustomer) =
                CustomerRepresentation(customer.firstName, customer.lastName)
    }
}
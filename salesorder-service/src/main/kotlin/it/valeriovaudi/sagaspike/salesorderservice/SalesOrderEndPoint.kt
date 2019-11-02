package it.valeriovaudi.sagaspike.salesorderservice

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
            it.bodyToMono(CreateSalesOrderRequest::class.java)
                    .flatMap { newSalesOrderGateway.newSalesOrder(it) }
                    .flatMap { ok().body(BodyInserters.fromObject(it)) }
        }

        GET("/sales-order/{salesOrderId}") {
            getSalesOrder.execute(it.pathVariable("salesOrderId"))
                    .flatMap { ok().body(BodyInserters.fromObject(it)) }
        }
    }
}
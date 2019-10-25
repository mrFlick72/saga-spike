package it.valeriovaudi.sagaspike.salesorderservice

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono
import java.math.BigDecimal

data class SalesOrder(@Id var id: String? = null, var customer: Customer)

data class Goods(@Id var id: String? = null, var salesOrderId: String, var barcode: String, var name: String, var quantity: Int, var price: Money)

data class Money(var price: BigDecimal, var currency: String) {
    constructor() : this(BigDecimal.ZERO, "")

    companion object {
        fun zero() = Money()
    }
}

data class Customer(var firstName: String, var lastName: String) {
    constructor() : this("", "")
}

interface SalesOrderRepository : ReactiveMongoRepository<SalesOrder, String>


class AddGoodsToSalesOrder() {

    fun execute(salesOrderId: String, barcode: String, quantity: Int) = Mono.just(TODO())

    fun undo(salesOrderId: String, barcode: String, quantity: Int) = Mono.just(TODO())
}
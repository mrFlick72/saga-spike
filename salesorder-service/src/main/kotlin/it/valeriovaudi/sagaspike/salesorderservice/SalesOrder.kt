package it.valeriovaudi.sagaspike.salesorderservice

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono
import java.io.Serializable
import java.math.BigDecimal

data class SalesOrder(@Id var id: String? = null, var customer: Customer)

data class Goods(@Id var id: String? = null, var salesOrderId: String, var barcode: String, var name: String, var quantity: Int, var price: Money) : Serializable

data class Money(var price: BigDecimal, var currency: String) : Serializable {
    constructor() : this(BigDecimal.ZERO, "")

    companion object {
        fun zero() = Money()
    }
}

data class Customer(var firstName: String, var lastName: String) {
    constructor() : this("", "")
}

interface SalesOrderRepository : ReactiveMongoRepository<SalesOrder, String>

interface GoodsRepository : ReactiveMongoRepository<Goods, String>
package it.valeriovaudi.sagaspike.salesorderservice

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.Serializable
import java.math.BigDecimal

data class SalesOrder(val id: String, val customer: SalesOrderCustomer, val goods: List<Goods>, val total: Money)

data class SalesOrderCustomer(@Id var id: String? = null, var firstName: String, var lastName: String)

data class Goods(@Id var id: String? = null, var salesOrderId: String, var barcode: String, var name: String, var quantity: Int, var price: Money) : Serializable

data class Money(var value: BigDecimal, var currency: String) : Serializable {
    constructor() : this(BigDecimal.ZERO, "")
}

interface SalesOrderCustomerRepository : ReactiveMongoRepository<SalesOrderCustomer, String>

interface GoodsRepository : ReactiveMongoRepository<Goods, String> {
    fun findAllBySalesOrderId(salesOrderId: String): Flux<Goods>
}

@Service
class GetSalesOrder(private val salesOrderCustomerRepository: SalesOrderCustomerRepository,
                    private val goodsRepository: GoodsRepository) {

    fun execute(salesOrderId: String): Mono<SalesOrder> {
        return Mono.zip(
                salesOrderCustomerRepository.findById(salesOrderId),
                goodsRepository.findAllBySalesOrderId(salesOrderId).collectList(),
                { customer: SalesOrderCustomer, goods: List<Goods> -> SalesOrder(salesOrderId, customer, goods, totalFor(goods)) })

    }

    private fun totalFor(goods: List<Goods>) =
            goods.map { item ->
                Money(item.price.value.multiply(BigDecimal(item.quantity)), item.price.currency)
            }.reduce { acc, money ->
                Money(acc.value.add(money.value), acc.currency)
            }
}
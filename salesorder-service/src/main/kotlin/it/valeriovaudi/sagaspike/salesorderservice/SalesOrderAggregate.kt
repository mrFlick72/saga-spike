package it.valeriovaudi.sagaspike.salesorderservice

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.Serializable
import java.math.BigDecimal

data class SalesOrderAggregate(val id: String, val customer: CustomerSalesOrder, val goods: List<SalesOrderGoods>, val total: Money)

data class CustomerSalesOrder(@Id var id: String? = null, var firstName: String, var lastName: String)
data class SalesOrderGoods(@Id var id: String? = null, var salesOrderId: String, var barcode: String, var name: String, var quantity: Int, var price: Money) : Serializable

data class Money(var value: BigDecimal, var currency: String) : Serializable {
    constructor() : this(BigDecimal.ZERO, "")
}

interface SalesOrderCustomerRepository : ReactiveMongoRepository<CustomerSalesOrder, String>

interface GoodsRepository : ReactiveMongoRepository<SalesOrderGoods, String> {
    fun findAllBySalesOrderId(salesOrderId: String): Flux<SalesOrderGoods>
}

@Service
class GetSalesOrder(private val salesOrderCustomerRepository: SalesOrderCustomerRepository,
                    private val goodsRepository: GoodsRepository) {

    fun execute(salesOrderId: String): Mono<SalesOrderAggregate> {
        return Mono.zip(
                salesOrderCustomerRepository.findById(salesOrderId),
                goodsRepository.findAllBySalesOrderId(salesOrderId).collectList(),
                { customer: CustomerSalesOrder, goods: List<SalesOrderGoods> -> SalesOrderAggregate(salesOrderId, customer, goods, totalFor(goods)) })

    }

    private fun totalFor(goods: List<SalesOrderGoods>) =
            goods.map { item ->
                Money(item.price.value.multiply(BigDecimal(item.quantity)), item.price.currency)
            }.reduce { acc, money ->
                Money(acc.value.add(money.value), acc.currency)
            }
}
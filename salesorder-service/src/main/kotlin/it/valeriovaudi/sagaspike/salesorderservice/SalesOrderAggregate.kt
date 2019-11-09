package it.valeriovaudi.sagaspike.salesorderservice

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.util.function.Tuple3
import java.io.Serializable
import java.math.BigDecimal
import java.util.*

data class SalesOrderAggregate(val id: String, val customer: CustomerSalesOrder, val goods: List<SalesOrderGoods>, val total: Money, val status: OrderStatus)

data class SalesOrderStatus(@Id var id: String? = null, var salesOrderId: String, var status: OrderStatus)
enum class OrderStatus { PENDING, COMPLETE, ABBORTED }

data class CustomerSalesOrder(@Id var id: String? = null, var firstName: String, var lastName: String)
data class SalesOrderGoods(@Id var id: String? = null, var salesOrderId: String, var barcode: String, var name: String, var quantity: Int, var price: Money) : Serializable {
    companion object {
        fun empty() = SalesOrderGoods(salesOrderId = "", barcode = "", name = "", quantity = 0, price = Money())
    }
}

data class Money(var value: BigDecimal, var currency: String) : Serializable {
    constructor() : this(BigDecimal.ZERO, "")
}

interface SalesOrderStatusRepository : ReactiveMongoRepository<SalesOrderStatus, String> {
    fun findAllBySalesOrderId(salesOrderId: String): Flux<SalesOrderStatus>
}

interface SalesOrderCustomerRepository : ReactiveMongoRepository<CustomerSalesOrder, String>

interface GoodsRepository : ReactiveMongoRepository<SalesOrderGoods, String> {
    fun findAllBySalesOrderId(salesOrderId: String): Flux<SalesOrderGoods>
}

@Service
class GetSalesOrder(private val salesOrderCustomerRepository: SalesOrderCustomerRepository,
                    private val goodsRepository: GoodsRepository,
                    private val salesOrderStatusRepository: SalesOrderStatusRepository) {

    fun execute(salesOrderId: String): Mono<SalesOrderAggregate> {
        return Mono.zip(
                salesOrderCustomerRepository.findById(salesOrderId),

                goodsRepository.findAllBySalesOrderId(salesOrderId).collectList()
                        .switchIfEmpty(emptyList<SalesOrderGoods>().toMono()),

                salesOrderStatusRepository.findAllBySalesOrderId(salesOrderId).collectList()
                        .switchIfEmpty(emptyList<SalesOrderStatus>().toMono()))
                .map { tuple: Tuple3<CustomerSalesOrder, List<SalesOrderGoods>, List<SalesOrderStatus>> ->
                    SalesOrderAggregate(salesOrderId, tuple.t1, tuple.t2, totalFor(tuple.t2), statusFor(tuple.t3))
                }
    }

    private fun statusFor(status: List<SalesOrderStatus>): OrderStatus {
        println("status $status")
        return Optional.ofNullable(status.find { it.status.equals(OrderStatus.ABBORTED) })
                .or { Optional.ofNullable(status.find { it.status.equals(OrderStatus.COMPLETE) }) }
                .orElse(status.find { it.status.equals(OrderStatus.PENDING) })
                .status

    }

    private fun totalFor(goods: List<SalesOrderGoods>) =
            if (goods.isNotEmpty()) {
                goods.map { item ->
                    Money(item.price.value.multiply(BigDecimal(item.quantity)), item.price.currency)
                }.reduce { acc, money ->
                    Money(acc.value.add(money.value), acc.currency)
                }
            } else {
                Money(BigDecimal.ZERO, "EUR")
            }
}
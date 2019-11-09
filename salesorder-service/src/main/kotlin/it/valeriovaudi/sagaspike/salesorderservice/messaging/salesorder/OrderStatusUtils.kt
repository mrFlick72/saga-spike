package it.valeriovaudi.sagaspike.salesorderservice.messaging.salesorder

import it.valeriovaudi.sagaspike.salesorderservice.OrderStatus
import it.valeriovaudi.sagaspike.salesorderservice.SalesOrderStatus
import it.valeriovaudi.sagaspike.salesorderservice.SalesOrderStatusRepository
import org.springframework.messaging.MessageHeaders
import reactor.core.publisher.Mono

object OrderStatusUtils {

    fun setSalesOrderStatusTo(orderStatus: OrderStatus, headers: MessageHeaders,
                              salesOrderStatusRepository: SalesOrderStatusRepository): Mono<SalesOrderStatus> =
            salesOrderStatusRepository.save(SalesOrderStatus(salesOrderId = headers["sales-order-id"] as String, status = orderStatus))

}
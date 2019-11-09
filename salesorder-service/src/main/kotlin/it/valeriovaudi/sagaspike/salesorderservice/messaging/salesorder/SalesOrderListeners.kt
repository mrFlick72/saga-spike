package it.valeriovaudi.sagaspike.salesorderservice.messaging.salesorder

import it.valeriovaudi.sagaspike.salesorderservice.CustomerSalesOrder
import it.valeriovaudi.sagaspike.salesorderservice.SalesOrderCustomerRepository
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.Output
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.reactive.FluxSender
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.toMono

class CreateSalesOrderListener(private val salesOrderCustomerRepository: SalesOrderCustomerRepository) {

    @StreamListener
    fun execute(@Input("createSalesOrderRequestChannel") input: Flux<Message<NewSalesOrderRequest>>,
                @Output("createSalesOrderResponseChannel") output: FluxSender) {
        output.send(
                input.flatMap { message ->
                    message.payload.let { payload ->
                        salesOrderCustomerRepository.save(CustomerSalesOrder(id = payload.salesOrderId, firstName = payload.customer.firstName, lastName = payload.customer.lastName))
                                .flatMap { salesOrder ->
                                    MessageBuilder
                                            .withPayload(payload.goods.mapIndexed { index, goods -> newGoodsRequest(goods) })
                                            .copyHeaders(mapOf("sales-order-id" to salesOrder.id))
                                            .build()
                                            .toMono()
                                }
                    }
                }
        )
    }

    private fun newGoodsRequest(goods: GoodsRequest): GoodsRequest {
        return GoodsRequest(goods.catalogId, barcode = goods.barcode, quantity = goods.quantity)
    }

}

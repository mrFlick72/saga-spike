package it.valeriovaudi.sagaspike.salesorderservice

import it.valeriovaudi.sagaspike.salesorderservice.messaging.salesorder.GoodsRequest
import it.valeriovaudi.sagaspike.salesorderservice.messaging.salesorder.NewSalesOrderRequest
import it.valeriovaudi.sagaspike.salesorderservice.messaging.salesorder.SalesOrderMessageChannel
import org.hamcrest.core.Is
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.integration.channel.FluxMessageChannel
import org.springframework.messaging.support.MessageBuilder.withPayload
import org.springframework.test.context.junit4.SpringRunner
import reactor.core.publisher.toFlux
import java.time.Duration
import java.util.*

@SpringBootTest
@RunWith(SpringRunner::class)
class CreateCustomerSalesOrderAggregateRequestListenerTest {

    @Autowired
    lateinit var createSalesOrderResponseChannel: FluxMessageChannel

    @Autowired
    lateinit var salesOrderMessageChannel: SalesOrderMessageChannel

    @Autowired
    lateinit var salesOrderCustomerRepository: SalesOrderCustomerRepository

    @Test
    fun `create a new salse order happy path`() {
        val salesOrderId = UUID.randomUUID().toString()
        val customer = CustomerSalesOrder(salesOrderId, "FIRST_NAME", "LAST_NAME")

        val message = withPayload(NewSalesOrderRequest(salesOrderId,
                CustomerRepresentation("FIRST_NAME", "LAST_NAME"),
                listOf(GoodsRequest(catalogId = "", barcode = "A_BARCODE", quantity = 10),
                        GoodsRequest(catalogId = "", barcode = "ANOTHER_BARCODE", quantity = 20))))
                .build()

        salesOrderMessageChannel.createSalesOrderRequestChannel().send(message)
        val actual = extractGoodsRequest()
        val salesOrder = salesOrderCustomerRepository.findById(salesOrderId).block(Duration.ofMinutes(1))

        assertThat(salesOrder, Is.`is`(customer))
        val expected = listOf(
                GoodsRequest(catalogId = "", barcode = "A_BARCODE", quantity = 10),
                GoodsRequest(catalogId = "", barcode = "ANOTHER_BARCODE", quantity = 20)
        )
        assertThat(actual, Is.`is`(expected))
    }

    fun extractGoodsRequest(): List<GoodsRequest> {
        return createSalesOrderResponseChannel.toFlux().blockFirst()!!.payload as List<GoodsRequest>
    }
}

package it.valeriovaudi.sagaspike.salesorderservice

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.core.Is
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.stream.test.binder.MessageCollector
import org.springframework.messaging.support.MessageBuilder.withPayload
import org.springframework.test.context.junit4.SpringRunner
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

/*

@SpringBootTest
@RunWith(SpringRunner::class)
class CreateSalesOrderListenerTest {

    @Autowired
    lateinit var messageCollector: MessageCollector

    @Autowired
    lateinit var salesOrderMessageChannel: SalesOrderMessageChannel

    @Autowired
    lateinit var salesOrderRepository: SalesOrderRepository

    val objectMapper: ObjectMapper = ObjectMapper()

    @Test
    fun `create a new salse order happy path`() {
        val customer = Customer("FIRST_NAME", "LAST_NAME")

        val salesOrderId = UUID.randomUUID().toString()
        val message = withPayload(CreateSalesOrderRequest(salesOrderId,
                Customer("FIRST_NAME", "LAST_NAME"),
                listOf(GoodsRequest(barcode = "A_BARCODE", quantity = 10),
                        GoodsRequest(barcode = "ANOTHER_BARCODE", quantity = 20))))
                .build()

        salesOrderMessageChannel.createSalesOrderRequestChannel().send(message)
        val actual = extractGoodsRequest()
        val salesOrder = salesOrderRepository.findById(salesOrderId).block(Duration.ofMinutes(1))

        assertThat(salesOrder!!.customer, Is.`is`(customer))
        val expected = listOf(GoodsRequest(salesOrderId = salesOrderId, barcode = "A_BARCODE", quantity = 10), GoodsRequest(salesOrderId = salesOrderId, barcode = "ANOTHER_BARCODE", quantity = 20))
        assertThat(actual, Is.`is`(expected))
    }

    fun extractGoodsRequest(): List<GoodsRequest> {
        val payload = messageCollector.forChannel(salesOrderMessageChannel.createSalesOrderResponseChannel())
                .poll(1000, TimeUnit.MILLISECONDS)
                .payload as String
        println(payload)
        return objectMapper.readValue(payload, object : TypeReference<List<GoodsRequest>>() {} )
    }
}*/

package it.valeriovaudi.sagaspike.salesorderservice

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.core.Is
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.stream.test.binder.MessageCollector
import org.springframework.messaging.support.MessageBuilder.withPayload
import org.springframework.test.context.junit4.SpringRunner
import java.util.concurrent.TimeUnit


@SpringBootTest
@RunWith(SpringRunner::class)
class CreateSalesOrderListenerTest {

    @Autowired
    lateinit var messageCollector: MessageCollector

    @Autowired
    lateinit var salesOrderMessageChannel: SalesOrderMessageChannel

    val objectMapper: ObjectMapper = ObjectMapper()

    @Test
    fun `create a new salse order happy path`() {
        val customer = Customer("FIRST_NAME", "LAST_NAME")

        val message = withPayload(CreateSalesOrderRequest(Customer("FIRST_NAME", "LAST_NAME"), emptyList())).build()
        salesOrderMessageChannel.createSalesOrderRequestChannel().send(message)

        val payload = messageCollector.forChannel(salesOrderMessageChannel.createSalesOrderResponseChannel())
                .poll(1000, TimeUnit.MILLISECONDS)
                .payload as String

        val actual = objectMapper.readValue(payload, SalesOrder::class.java)
        println(actual)
        assertNotNull(actual.id)
        assertThat(actual.customer, Is.`is`(customer))
    }
}
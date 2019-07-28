package it.valeriovaudi.sagaspike.inventoryservice

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.core.Is
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.stream.test.binder.MessageCollector
import org.springframework.messaging.support.MessageBuilder
import org.springframework.test.context.junit4.SpringRunner
import java.util.concurrent.TimeUnit


@SpringBootTest
@RunWith(SpringRunner::class)
class ReserveGoodsListenerTest {

    @Autowired
    lateinit var inventoryMessageChannel: InventoryMessageChannel

    @Autowired
    lateinit var messageCollector: MessageCollector

    val objectMapper: ObjectMapper = ObjectMapper()

    @Autowired
    lateinit var inventoryRepository: InventoryRepository

//    @Autowired
//    @Qualifier("reserveGoodsRequestChannel.reserveGoodsRequest.errors")
//    lateinit var errorChannel: SubscribableChannel

    val goods = Goods("barcode", "A_GOODS_NAME", availability = 10)
    val reservedGoodsQuantity = ReservedGoodsQuantity("barcode", quantity = 5)

    @Before
    fun setUp() {
        inventoryRepository.save(goods).block()
    }

    @Test
    fun `reserve a goods`() {
        val message = MessageBuilder.withPayload(ReserveGoodsQuantity("barcode", 5)).build()

        inventoryMessageChannel.reserveGoodsRequestChannel().send(message)

        val response = messageCollector.forChannel(inventoryMessageChannel.reserveGoodsResponseChannel())
                .poll(1000, TimeUnit.MILLISECONDS)
        print("response:  $response")
        assertNotNull(response)
        assertThat(response.payload as String, Is.`is`(objectMapper.writeValueAsString(reservedGoodsQuantity)))
    }

    @Test
    @Ignore("I do not able to test error channel messages.... for now")
    fun `reserve a goods goes in error due to goods unavailability`() {
        val message = MessageBuilder.withPayload(ReserveGoodsQuantity("barcode", 15)).build()

        inventoryMessageChannel.reserveGoodsRequestChannel().send(message)

    /*    val response = messageCollector.forChannel(errorChannel)
                .poll(1000, TimeUnit.MILLISECONDS)
        print("response:  $response")
        assertNotNull(response)
        assertThat(response.payload as String, Is.`is`(objectMapper.writeValueAsString(reservedGoodsQuantity)))*/
    }
}
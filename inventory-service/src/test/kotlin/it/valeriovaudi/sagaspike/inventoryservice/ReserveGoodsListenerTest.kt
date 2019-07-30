package it.valeriovaudi.sagaspike.inventoryservice

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.core.Is
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.stream.test.binder.MessageCollector
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.messaging.support.MessageBuilder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import java.util.concurrent.TimeUnit


@DirtiesContext
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

    @Autowired
    lateinit var errorLogger: ErrorLogger


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
    fun `reserve a goods goes in error due to goods unavailability`() {
        val message = MessageBuilder.withPayload(ReserveGoodsQuantity("barcode", 15)).build()

        inventoryMessageChannel.reserveGoodsRequestChannel().send(message)

        val response = messageCollector.forChannel(inventoryMessageChannel.reserveGoodsResponseChannel())
                .poll(1000, TimeUnit.MILLISECONDS)

        print("errorLogger:  $errorLogger")
        print("response:  $response")
        assertNull(response)

        verify(errorLogger).log(any())
    }

}

@Configuration
class MockingBeans {

    @Bean
    @Primary
    fun mockerdErrorLogger() = mock(ErrorLogger::class.java)
}
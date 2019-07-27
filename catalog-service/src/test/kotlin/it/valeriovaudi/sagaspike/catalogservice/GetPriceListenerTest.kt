package it.valeriovaudi.sagaspike.catalogservice

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.core.Is
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.stream.test.binder.MessageCollector
import org.springframework.messaging.support.MessageBuilder.withPayload
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.TimeUnit


@SpringBootTest
@RunWith(SpringRunner::class)
class GetPriceListenerTest {

    @Autowired
    lateinit var catalogMessageChannel: CatalogMessageChannel

    @Autowired
    lateinit var messageCollector: MessageCollector

    val objectMapper: ObjectMapper = ObjectMapper()

    @Autowired
    lateinit var catalogRepository: CatalogRepository

    val catalogId = UUID.randomUUID().toString()
    val goodsWithPrice = GoodsWithPrice(Goods("barcode", "A_GOODS_NAME"), Price(BigDecimal.ONE, "EUR"))

    @Before
    fun setUp() {

        catalogRepository.save(Catalog(catalogId, listOf(goodsWithPrice)))
                .block()
    }

    @Test
    fun `get a goods with price`() {
        val message = withPayload(GoodsWithPriceMessageRequest(catalogId, "barcode")).build()

        catalogMessageChannel.goodsPricingRequestChannel().send(message)

        val response = messageCollector.forChannel(catalogMessageChannel.goodsPricingResponseChannel())
                .poll(1000, TimeUnit.MILLISECONDS)
        print("response:  $response")
        assertNotNull(response)
        assertThat(response.payload as String, Is.`is`(objectMapper.writeValueAsString(goodsWithPrice)))
    }
}
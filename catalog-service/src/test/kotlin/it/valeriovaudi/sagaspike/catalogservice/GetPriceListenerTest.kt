package it.valeriovaudi.sagaspike.catalogservice

import org.junit.Assert.assertNotNull
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


@SpringBootTest
@RunWith(SpringRunner::class)
class GetPriceListenerTest {

    @Autowired
    lateinit var catalogMessageChannel: CatalogMessageChannel

    @Autowired
    lateinit var messageCollector: MessageCollector

    @Autowired
    lateinit var catalogRepository: CatalogRepository

    val catalogId = UUID.randomUUID().toString()

    @Before
    fun setUp() {
        val goodsWithPrice = GoodsWithPrice(Goods("barcode", "A_GOODS_NAME"), Price(BigDecimal.ONE, "EUR"))

        catalogRepository.save(Catalog(catalogId, listOf(goodsWithPrice)))
                .block()
    }

    @Test
    fun `get a goods with price`() {
        println("catalogRepository.findAll().collectList()")
        println(catalogRepository.findAll().collectList().block())

        val message = withPayload(GoodsWithPriceMessageRequest(catalogId, "barcode")).build()

        catalogMessageChannel.goodsPricingRequestChannel().send(message)

        Thread.sleep(100)
        val response = messageCollector.forChannel(catalogMessageChannel.goodsPricingResponseChannel()).poll()

        assertNotNull(response)

        println("response: ${response.payload}")
    }
}
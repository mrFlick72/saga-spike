package it.valeriovaudi.sagaspike.inventoryservice

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import reactor.core.publisher.toMono
import reactor.test.StepVerifier

@RunWith(MockitoJUnitRunner::class)
class ReserveGoodsTest {

    @Mock
    lateinit var inventoryRepository: InventoryRepository

    @Test
    fun `reserve available goods`() {

        val reserveGoods = ReserveGoods(inventoryRepository)

        given(inventoryRepository.findByBarcode("A_BARCODE"))
                .willReturn(Goods(barcode = "A_BARCODE", name = "A_GOODS", availability = 10).toMono())

        given(inventoryRepository.save(Goods(barcode = "A_BARCODE", name = "A_GOODS", availability = 5)))
                .willReturn(Goods(barcode = "A_BARCODE", name = "A_GOODS", availability = 5).toMono())

        val execute = reserveGoods.execute("A_BARCODE", 5)

        StepVerifier.create(execute)
                .expectNext(Goods(barcode = "A_BARCODE", name = "A_GOODS", availability = 5))
                .expectComplete()
                .verify()
    }

    @Test
    fun `reserve an unavailable goods`() {

        val reserveGoods = ReserveGoods(inventoryRepository)

        given(inventoryRepository.findByBarcode("A_BARCODE"))
                .willReturn(Goods(barcode = "A_BARCODE", name = "A_GOODS", availability = 10).toMono())

        val execute = reserveGoods.execute("A_BARCODE", 15)

        StepVerifier.create(execute)
                .expectError(NoGoodsAvailabilityException::class.java)
                .verify()
    }
}
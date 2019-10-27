package it.valeriovaudi.sagaspike.catalogservice

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class Initializer(private val catalogRepository: CatalogRepository) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        println("INIT")

        val goodsList = listOf(
                GoodsWithPrice(Goods("A_BARCODE_1", "A_GOODS_1"), Price(BigDecimal.TEN, "EUR")),
                GoodsWithPrice(Goods("A_BARCODE_2", "A_GOODS_2"), Price(BigDecimal.TEN, "EUR")),
                GoodsWithPrice(Goods("A_BARCODE_3", "A_GOODS_3"), Price(BigDecimal.TEN, "EUR")),
                GoodsWithPrice(Goods("A_BARCODE_4", "A_GOODS_4"), Price(BigDecimal.TEN, "EUR"))
        )

        val catalog = Catalog("CATALOG01", goodsList)


        catalogRepository.deleteAll()
                .thenMany(catalogRepository.save(catalog))
                .blockLast()
    }

}
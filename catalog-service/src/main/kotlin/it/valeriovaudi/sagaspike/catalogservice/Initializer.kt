package it.valeriovaudi.sagaspike.catalogservice

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import reactor.core.publisher.toFlux
import java.math.BigDecimal

@Component
class Initializer(private val catalogRepository: CatalogRepository) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        println("INIT")

        val catalogs = listOf(
                Catalog("CATALOG01", listOf(
                        GoodsWithPrice(Goods("A_BARCODE_1", "A_GOODS_1"), Price(BigDecimal.TEN, "EUR")),
                        GoodsWithPrice(Goods("A_BARCODE_2", "A_GOODS_2"), Price(BigDecimal.TEN, "EUR")),
                        GoodsWithPrice(Goods("A_BARCODE_3", "A_GOODS_3"), Price(BigDecimal.TEN, "EUR")),
                        GoodsWithPrice(Goods("A_BARCODE_4", "A_GOODS_4"), Price(BigDecimal.TEN, "EUR"))
                )),
                Catalog("CATALOG02", listOf(
                        GoodsWithPrice(Goods("A_BARCODE_1", "A_GOODS_1"), Price(BigDecimal("10.50"), "EUR")),
                        GoodsWithPrice(Goods("A_BARCODE_2", "A_GOODS_2"), Price(BigDecimal("23"), "EUR")),
                        GoodsWithPrice(Goods("A_BARCODE_3", "A_GOODS_3"), Price(BigDecimal("23"), "EUR")),
                        GoodsWithPrice(Goods("A_BARCODE_4", "A_GOODS_4"), Price(BigDecimal("9.50"), "EUR"))
                ))
        )

        catalogRepository.deleteAll()
                .thenMany(catalogs.toFlux().flatMap { catalog -> catalogRepository.save(catalog) })
                .blockLast()
    }

}
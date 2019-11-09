package it.valeriovaudi.sagaspike.catalogservice

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.math.BigDecimal

data class Goods(val barcode: String, val name: String)

data class Price(val price: BigDecimal, val currency: String)

data class Catalog(@Id val id: String, val goods: List<GoodsWithPrice>)

data class GoodsWithPrice(val goods: Goods, val price: Price) {

    companion object {
        fun empty() = GoodsWithPrice(Goods("", ""), Price(BigDecimal.ZERO, ""))
    }
}
class NoGoodsInCatalogException(message: String) : RuntimeException(message)

interface CatalogRepository : ReactiveMongoRepository<Catalog, String>


class FindGoodsInCatalog(private val catalogRepository: CatalogRepository) {

    fun findFor(catalogId: String, barcode: String) =
            catalogRepository.findAll()
                    .filter { catalog -> catalog.id == catalogId }
                    .flatMapIterable { catalog -> catalog.goods }
                    .filter { goodsWithPrice -> goodsWithPrice.goods.barcode == barcode }
                    .toMono()
                    .switchIfEmpty(Mono.error(NoGoodsInCatalogException("The Goods $barcode is not in any catalog")))
}
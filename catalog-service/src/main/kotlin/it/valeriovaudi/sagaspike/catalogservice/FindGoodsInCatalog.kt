package it.valeriovaudi.sagaspike.catalogservice

class FindGoodsInCatalog(private val catalogRepository: CatalogRepository){

    fun findFor(catalogId : String, barcode : String) =
            catalogRepository.findAll()
                    .filter { catalog -> catalog.id == catalogId }
                    .flatMapIterable { catalog -> catalog.goods }
                    .filter { goodsWithPrice -> goodsWithPrice.goods.barcode == barcode }
}
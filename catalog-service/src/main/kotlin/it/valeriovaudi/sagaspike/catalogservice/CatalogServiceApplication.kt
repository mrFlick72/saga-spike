package it.valeriovaudi.sagaspike.catalogservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.context.annotation.Bean

@SpringBootApplication
@EnableBinding(CatalogMessageChannel::class)
class CatalogServiceApplication {

    @Bean
    fun findGoodsInCatalog(catalogRepository: CatalogRepository) = FindGoodsInCatalog(catalogRepository)

    @Bean
    fun getPriceListener(findGoodsInCatalog: FindGoodsInCatalog) = GetPriceListener(findGoodsInCatalog)
}

fun main(args: Array<String>) {
    runApplication<CatalogServiceApplication>(*args)
}
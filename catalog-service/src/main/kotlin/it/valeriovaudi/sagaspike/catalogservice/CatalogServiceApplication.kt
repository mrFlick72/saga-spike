package it.valeriovaudi.sagaspike.catalogservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.context.annotation.Bean
import org.springframework.integration.config.GlobalChannelInterceptor

@SpringBootApplication
@EnableBinding(CatalogMessageChannel::class)
class CatalogServiceApplication {

    @Bean
    fun findGoodsInCatalog(catalogRepository: CatalogRepository) = FindGoodsInCatalog(catalogRepository)

    @Bean
    fun getPriceListener(findGoodsInCatalog: FindGoodsInCatalog) = GetPriceListener(findGoodsInCatalog)

    @Bean
    @GlobalChannelInterceptor
    fun executionIdPropagatorChannelInterceptor() = ExecutionIdPropagatorChannelInterceptor()
}

fun main(args: Array<String>) {
    runApplication<CatalogServiceApplication>(*args)
}
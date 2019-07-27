package it.valeriovaudi.sagaspike.catalogservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.support.MessageBuilder.withPayload
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.math.BigDecimal

@EnableBinding(CatalogMessageChannel::class)
@SpringBootApplication
class CatalogServiceApplication

fun main(args: Array<String>) {
    runApplication<CatalogServiceApplication>(*args)
}

data class Goods(val barcode: String, val name: String)

data class Price(val price: BigDecimal, val currency: String)

data class Catalog(@Id val id: String, val goods: List<GoodsWithPrice>)

data class GoodsWithPrice(val goods: Goods, val price: Price)

data class GoodsWithPriceMessageRequest(val catalogId: String, val barcode: String)

interface CatalogRepository : ReactiveMongoRepository<Catalog, String>


@Component
class GetPriceListener(private val catalogRepository: CatalogRepository) {

    @StreamListener("goodsPricingRequestChannel")
    @SendTo("goodsPricingResponseChannel")
    fun handleGoodsPriceRequest(message: Message<GoodsWithPriceMessageRequest>): Mono<Message<GoodsWithPrice>> {

        println("handleGoodsPriceRequest invoked")
        println("message $message")

        return catalogRepository.findAll()
                .log()
                .filter { it.id == message.payload.catalogId }
                .flatMapIterable { it.goods }
                .filter { it.goods.barcode == message.payload.barcode }
                .map { withPayload(it).build() }
                .log()
                .toMono()
    }

}
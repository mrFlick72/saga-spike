package it.valeriovaudi.sagaspike.inventoryservice

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono
import reactor.util.Loggers

@SpringBootApplication
class InventoryServiceApplication {

    @Bean
    fun reserveGoods(inventoryRepository: InventoryRepository) = ReserveGoods(inventoryRepository)
}

fun main(args: Array<String>) {
    runApplication<InventoryServiceApplication>(*args)
}

interface InventoryRepository : ReactiveMongoRepository<Goods, String> {
    fun findByBarcode(barcode: String): Mono<Goods>
}

class NoGoodsAvailabilityException(message: String) : RuntimeException(message)

data class Goods(@Id val barcode: String, val name: String, val availability: Int)

class ReserveGoods(private val inventoryRepository: InventoryRepository) {

    fun execute(barcode: String, quantity: Int) =
            inventoryRepository.findByBarcode(barcode)
                    .filter { goods -> filterAvailableGoods(goods, quantity) }
                    .flatMap { goods -> reserveGoods(goods, quantity) }
                    .switchIfEmpty(Mono.error(NoGoodsAvailabilityException("The Goods $barcode availability is not enough")))

    private fun reserveGoods(goods: Goods, quantity: Int) =
            inventoryRepository.save(Goods(goods.barcode, goods.name, goods.availability - quantity))

    private fun filterAvailableGoods(goods: Goods, quantity: Int) =
            goods.availability >= quantity
}
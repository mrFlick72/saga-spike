package it.valeriovaudi.sagaspike.inventoryservice

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

data class Goods(@Id val barcode: String, val name: String, val availability: Int)

data class NotAvailableGoods(var barcode: String, var quantity: Int, var errorMessage: String)

class NoGoodsAvailabilityException(message: String) : RuntimeException(message)

@Transactional(readOnly = true)
interface InventoryRepository : ReactiveMongoRepository<Goods, String> {
    fun findByBarcode(barcode: String): Mono<Goods>
}

class ReserveGoods(private val inventoryRepository: InventoryRepository) {

    fun execute(barcode: String, quantity: Int) =
            inventoryRepository.findByBarcode(barcode)
                    .filter { goods -> filterAvailableGoods(goods, quantity) }
                    .flatMap { goods -> reserveGoods(goods, quantity) }
                    .switchIfEmpty(Mono.error(NoGoodsAvailabilityException("The Goods $barcode availability is not enough")))


    fun undo(barcode: String, quantity: Int) =
            inventoryRepository.findByBarcode(barcode)
                    .flatMap { goods -> unReserveGoods(goods, quantity) }


    private fun reserveGoods(goods: Goods, quantity: Int) =
            inventoryRepository.save(Goods(goods.barcode, goods.name, goods.availability - quantity))

    private fun unReserveGoods(goods: Goods, quantity: Int) =
            inventoryRepository.save(Goods(goods.barcode, goods.name, goods.availability + quantity))

    private fun filterAvailableGoods(goods: Goods, quantity: Int) =
            goods.availability >= quantity
}
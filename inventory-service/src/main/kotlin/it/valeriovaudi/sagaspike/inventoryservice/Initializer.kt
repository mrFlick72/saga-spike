package it.valeriovaudi.sagaspike.inventoryservice

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class Initializer(private val inventoryRepository: InventoryRepository) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        println("INIT")
        val goodsList = listOf(
                Goods("A_BARCODE_1", "A_GOODS_1", 50),
                Goods("A_BARCODE_2", "A_GOODS_2", 50),
                Goods("A_BARCODE_3", "A_GOODS_3", 50),
                Goods("A_BARCODE_4", "A_GOODS_4", 50)
        )
        inventoryRepository.deleteAll()
                .thenMany(inventoryRepository.saveAll(goodsList))
                .blockLast()
    }

}
package it.valeriovaudi.sagaspike.inventoryservice

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.Output
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.reactive.FluxSender
import org.springframework.context.annotation.Bean
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.SubscribableChannel
import org.springframework.messaging.support.MessageBuilder.withPayload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@EnableBinding(InventoryMessageChannel::class)
@SpringBootApplication
class InventoryServiceApplication {

    @Bean
    fun reserveGoods(inventoryRepository: InventoryRepository) = ReserveGoods(inventoryRepository)

    @Bean
    fun reserveGoodsListener(reserveGoods: ReserveGoods) = ReserveGoodsListener(reserveGoods)
}

fun main(args: Array<String>) {
    runApplication<InventoryServiceApplication>(*args)
}

@Component
class Initializer(private val inventoryRepository: InventoryRepository) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        val goodsList = listOf(
                Goods("A_BARCODE_1", "A_GOODS_1", 10),
                Goods("A_BARCODE_2", "A_GOODS_2", 10),
                Goods("A_BARCODE_3", "A_GOODS_3", 10),
                Goods("A_BARCODE_4", "A_GOODS_4", 10)
        )
        inventoryRepository.deleteAll()
                .thenMany(inventoryRepository.saveAll(goodsList))
                .blockLast()
    }

}

@Transactional(readOnly = true)
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


interface InventoryMessageChannel {

    @Input
    fun reserveGoodsRequestChannel(): SubscribableChannel

    @Output
    fun reserveGoodsResponseChannel(): MessageChannel

}

class ReserveGoodsListener(private val reserveGoods: ReserveGoods) {

    @StreamListener
    fun handleGoodsPriceRequest(@Input("reserveGoodsRequestChannel") input: Flux<Message<ReserveGoodsQuantity>>,
                                @Output("reserveGoodsResponseChannel") output: FluxSender) {
        output.send(input.flatMap { message ->
            message.payload.let { reserveGoods.execute(it.barcode, it.quantity) }
                    .map { withPayload(ReservedGoodsQuantity(message.payload.barcode, message.payload.quantity)).build() }

        })
    }

    @StreamListener("errorChannel")
    fun error(message: Message<*>) {
        println("Handling ERROR: $message")
    }
}

data class ReserveGoodsQuantity(val barcode: String, val quantity: Int)

data class ReservedGoodsQuantity(val barcode: String, val quantity: Int)
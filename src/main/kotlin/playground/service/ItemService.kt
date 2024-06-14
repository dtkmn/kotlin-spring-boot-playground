package playground.service

import org.springframework.stereotype.Service
import playground.entity.Item
import playground.repository.ItemRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ItemService(private val itemRepository: ItemRepository) {

    fun getAllItems(): Flux<Item> = itemRepository.findAll()

    fun getItemById(id: Long): Mono<Item> = itemRepository.findById(id)

    fun createItem(item: Item): Mono<Item> = itemRepository.save(item)

    fun deleteItem(id: Long): Mono<Void> = itemRepository.deleteById(id)

}

package playground.controller

import org.springframework.web.bind.annotation.*
import playground.entity.Item
import playground.service.ItemService
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/items")
class ItemController(
    private val itemService: ItemService
) {

    @GetMapping
    fun getAllItems(): Flux<Item> = itemService.getAllItems()

    @GetMapping("/{id}")
    fun getItemById(@PathVariable id: Long): Mono<Item> = itemService.getItemById(id)

    @PostMapping
    fun createItem(@RequestBody item: Item): Mono<Item> = itemService.createItem(item)

    @DeleteMapping("/{id}")
    fun deleteItem(@PathVariable id: Long): Mono<Void> = itemService.deleteItem(id)

}

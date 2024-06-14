package playground.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import playground.entity.Item
import playground.repository.ItemRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class ItemServiceTest {

    private lateinit var itemService: ItemService
    private lateinit var itemRepository: ItemRepository

    @BeforeEach
    fun setUp() {
        itemRepository = mock(ItemRepository::class.java)
        itemService = ItemService(itemRepository)
    }

    @Test
    fun `test getAllItems`() {
        val items = listOf(
            Item(1L, "Item 1", "Description 1"),
            Item(2L, "Item 2", "Description 2")
        )
        `when`(itemRepository.findAll()).thenReturn(Flux.fromIterable(items))

        val result = itemService.getAllItems()

        StepVerifier.create(result)
            .expectNextSequence(items)
            .verifyComplete()
    }

    @Test
    fun `test getItemById`() {
        val item = Item(1L, "Item 1", "Description 1")
        `when`(itemRepository.findById(1L)).thenReturn(Mono.just(item))

        val result = itemService.getItemById(1L)

        StepVerifier.create(result)
            .expectNext(item)
            .verifyComplete()
    }

    @Test
    fun `test createItem`() {
        val item = Item(null, "New Item", "New Description")
        val savedItem = Item(1L, "New Item", "New Description")
        `when`(itemRepository.save(item)).thenReturn(Mono.just(savedItem))

        val result = itemService.createItem(item)

        StepVerifier.create(result)
            .expectNext(savedItem)
            .verifyComplete()
    }

    @Test
    fun `test deleteItem`() {
        `when`(itemRepository.deleteById(1L)).thenReturn(Mono.empty())
        val result = itemService.deleteItem(1L)
        StepVerifier.create(result)
            .verifyComplete()
    }
}
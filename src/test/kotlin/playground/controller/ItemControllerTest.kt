package playground.controller

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import playground.config.TestSecurityConfig
import playground.entity.Item
import playground.service.ItemService
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


@WebFluxTest(ItemController::class)
@Import(TestSecurityConfig::class)
class ItemControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var itemService: ItemService

    @Test
    fun `test getAllItems`() {
        val items = listOf(
            Item(1L, "Item 1", "Description 1"),
            Item(2L, "Item 2", "Description 2")
        )
        `when`(itemService.getAllItems()).thenReturn(Flux.fromIterable(items))

        webTestClient.get().uri("/api/items")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBodyList(Item::class.java)
            .hasSize(2)
            .contains(*items.toTypedArray())
    }

    @Test
    fun `test getItemById`() {
        val item = Item(1L, "Item 1", "Description 1")
        `when`(itemService.getItemById(1L)).thenReturn(Mono.just(item))

        webTestClient.get().uri("/api/items/1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody(Item::class.java)
            .isEqualTo(item)
    }

    @Test
    fun `test createItem`() {
        val item = Item(null, "New Item", "New Description")
        val savedItem = Item(1L, "New Item", "New Description")
        `when`(itemService.createItem(item)).thenReturn(Mono.just(savedItem))

        webTestClient.post().uri("/api/items")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(item)
            .exchange()
            .expectStatus().isOk
            .expectBody(Item::class.java)
            .isEqualTo(savedItem)
    }

    @Test
    fun `test deleteItem`() {
        `when`(itemService.deleteItem(1L)).thenReturn(Mono.empty())

        webTestClient.delete().uri("/api/items/1")
            .exchange()
            .expectStatus().isOk
            .expectBody(Void::class.java)
    }

}
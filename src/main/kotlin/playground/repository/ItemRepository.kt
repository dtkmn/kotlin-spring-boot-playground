package playground.repository

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Component
import playground.entity.Item

@Component
interface ItemRepository : ReactiveCrudRepository<Item, Long>

package playground.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("items")
data class Item (
    @Id
    val id: Long?,
    val name: String,
    val description: String
)
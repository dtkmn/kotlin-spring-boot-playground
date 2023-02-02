package playground

import com.fasterxml.jackson.annotation.JsonProperty

class Item2 {
    val id: String? = null
    val name: String? = null
    @JsonProperty("Born City")
    val bornCity: String? = null
    @JsonProperty("completion_time")
    val completionTime: String? = null
    val test: String? = null
}
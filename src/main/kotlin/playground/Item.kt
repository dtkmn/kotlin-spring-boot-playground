package playground

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat(with = [JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES])
class Item {
    val id: String? = null
    val name: String? = null
    @JsonProperty("Born City")
    val bornCity: String? = null
    @JsonProperty("completion_time")
    val completionTime: String? = null
}
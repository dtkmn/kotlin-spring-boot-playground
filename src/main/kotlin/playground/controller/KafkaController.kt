package playground.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import playground.service.KafkaService

@RestController
class KafkaController(
    private val kafkaService: KafkaService
) {

    @PostMapping("/send")
    fun sendMessage(
        @RequestParam topic: String,
        @RequestBody message: String
    ) {
        kafkaService.sendMessage(topic, message)
    }

}
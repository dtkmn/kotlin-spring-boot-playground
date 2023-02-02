package playground.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test")
class HtmlController {

    @GetMapping("/")
    fun blog(): String {
        return "blog"
    }

}

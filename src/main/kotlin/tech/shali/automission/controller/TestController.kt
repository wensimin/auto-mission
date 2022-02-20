package tech.shali.automission.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.RuntimeException

@RestController
@RequestMapping("test")
class TestController {
    @GetMapping
    fun test(): String {
        throw RuntimeException()
        return "admin!"
    }
}
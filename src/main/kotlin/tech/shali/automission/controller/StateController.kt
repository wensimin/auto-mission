package tech.shali.automission.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tech.shali.automission.service.StateService

@RestController
@RequestMapping("state")
class StateController(private val stateService: StateService) {
    @GetMapping
    suspend fun get() = stateService.state()
}
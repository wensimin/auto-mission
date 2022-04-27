package tech.shali.automission.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tech.shali.automission.service.TaskInstanceService

@RestController
@RequestMapping("task/instance")
class TaskInstanceController(private val taskInstanceService: TaskInstanceService) {
//    @GetMapping
//    fun get() {
//        taskInstanceService.getTask()
//    }
}
package tech.shali.automission.controller

import org.springframework.web.bind.annotation.*
import tech.shali.automission.pojo.TaskInstance
import tech.shali.automission.service.TaskInstanceService

@RestController
@RequestMapping("task/instance")
class TaskInstanceController(private val taskInstanceService: TaskInstanceService) {
    @GetMapping
    fun get(done: Boolean?): List<TaskInstance> {
        return taskInstanceService.findTask(done)
    }

    @PutMapping("{key}")
    fun stop(@PathVariable key: String) {
        taskInstanceService.stopTask(key)
    }

    @DeleteMapping
    fun clear()=taskInstanceService.clearDoneTask()

}
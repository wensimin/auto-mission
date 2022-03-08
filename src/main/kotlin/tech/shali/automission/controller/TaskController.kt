package tech.shali.automission.controller

import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*
import tech.shali.automission.entity.Task
import tech.shali.automission.pojo.DebugCodeVo
import tech.shali.automission.pojo.TaskQuery
import tech.shali.automission.pojo.TaskSave
import tech.shali.automission.service.TaskService
import javax.validation.Valid

@RestController
@RequestMapping("task")
class TaskController(
    private val taskService: TaskService
) {

    @GetMapping
    fun get(@Valid taskQuery: TaskQuery): Page<Task> {
        return taskService.find(taskQuery)
    }

    @GetMapping("{id}")
    fun getOne(@PathVariable id: String): Task {
        return taskService.findOne(id)
    }

    @PostMapping
    fun post(@RequestBody @Valid task: TaskSave) {
        taskService.save(task)
    }

    @PutMapping("{id}")
    fun put(@PathVariable id: String, enabled: Boolean) {
        taskService.switchTask(id, enabled)
    }

    @PostMapping("testCode")
    fun testCode(@RequestBody @Valid code: DebugCodeVo): String {
        return taskService.testCode(code)
    }
}
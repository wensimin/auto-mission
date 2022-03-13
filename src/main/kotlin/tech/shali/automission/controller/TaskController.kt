package tech.shali.automission.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*
import tech.shali.automission.entity.Task
import tech.shali.automission.pojo.DebugCodeVo
import tech.shali.automission.pojo.TaskQuery
import tech.shali.automission.pojo.TaskSave
import tech.shali.automission.service.TaskService
import java.io.File
import java.util.*
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
    fun getOne(@PathVariable id: UUID): Task {
        return taskService.findOne(id)
    }

    @PostMapping
    fun post(@RequestBody @Valid task: TaskSave) {
        taskService.save(task)
    }
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: UUID){
        taskService.deleteTask(id)
    }

    @PutMapping("{id}")
    fun put(@PathVariable id: UUID, enabled: Boolean) {
        taskService.switchTask(id, enabled)
    }


    @GetMapping("template")
    fun getTemplate(): String {
        return ClassPathResource("CodeTemplate").inputStream.bufferedReader().use { it.readText() }
    }
}
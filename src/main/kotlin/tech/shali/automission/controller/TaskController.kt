package tech.shali.automission.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*
import tech.shali.automission.entity.Task
import tech.shali.automission.pojo.TaskQuery
import tech.shali.automission.pojo.TaskSave
import tech.shali.automission.service.TaskService
import java.util.*
import javax.validation.Valid

@RestController
@RequestMapping("task")
class TaskController(
    private val taskService: TaskService
) {

    @GetMapping
    suspend fun get(@Valid taskQuery: TaskQuery): Page<Task> {
        return taskService.find(taskQuery)
    }

    @GetMapping("{id}")
    suspend fun getOne(@PathVariable id: UUID): Task {
        return taskService.findOne(id)
    }

    @PostMapping
    suspend fun post(@RequestBody @Valid task: TaskSave) {
        taskService.run {
            checkReady()
            save(task)
        }
    }
    @DeleteMapping("{id}")
    suspend fun delete(@PathVariable id: UUID) {
        taskService.run {
            checkReady()
            deleteTask(id)
        }
    }

    @PutMapping("{id}")
    fun put(@PathVariable id: UUID, enabled: Boolean) {
        taskService.run {
            checkReady()
            switchTask(id, enabled)
        }
    }


    @GetMapping("template")
    suspend fun getTemplate(): String {
        return ClassPathResource("CodeTemplate").inputStream.bufferedReader().use { it.readText() }
    }
}
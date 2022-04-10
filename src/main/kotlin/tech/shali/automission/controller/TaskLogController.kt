package tech.shali.automission.controller

import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tech.shali.automission.entity.TaskLog
import tech.shali.automission.pojo.TaskLogQuery
import tech.shali.automission.service.TaskLogService
import javax.validation.Valid

@RestController
@RequestMapping("taskLog")
class TaskLogController(private val taskLogService: TaskLogService) {

    @GetMapping
    suspend fun get(@Valid taskLogQuery: TaskLogQuery): Page<TaskLog> {
        return taskLogService.find(taskLogQuery)
    }

}
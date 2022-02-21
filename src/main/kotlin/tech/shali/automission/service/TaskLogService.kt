package tech.shali.automission.service

import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import tech.shali.automission.dao.TaskLogDao
import tech.shali.automission.entity.TaskLog
import tech.shali.automission.pojo.TaskLogQuery

@Service
class TaskLogService(private val taskLogDao: TaskLogDao) {

    fun find(taskLogQuery: TaskLogQuery): Page<TaskLog> {
        return taskLogDao.findAll(taskLogQuery.toSpecification<TaskLog>(), taskLogQuery.page.toPageRequest())
    }

}
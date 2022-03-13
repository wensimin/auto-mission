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

    fun log(log: TaskLog) {
        this.taskLogDao.save(log)
    }

    fun getLogger(taskId: String? = null): TaskLogger {
        return JdbcTaskLogger(this, taskId)
    }

    /**
     * 绑定task id 的logger
     */
    class JdbcTaskLogger(private val taskLogService: TaskLogService, private val id: String?) : TaskLogger {

        override fun log(level: TaskLogger.Level, message: String, taskId: String?) {
            taskLogService.log(TaskLog(level, message, taskId ?: id))
        }

    }

}
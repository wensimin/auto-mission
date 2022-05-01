package tech.shali.automission.service

import com.github.wensimin.jpaspecplus.findPageBySpec
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import tech.shali.automission.dao.TaskLogDao
import tech.shali.automission.entity.Task
import tech.shali.automission.entity.TaskLog
import tech.shali.automission.pojo.TaskLogQuery

@Service
class TaskLogService(private val taskLogDao: TaskLogDao) {

    fun find(taskLogQuery: TaskLogQuery): Page<TaskLog> {
        return taskLogDao.findPageBySpec(taskLogQuery, taskLogQuery.page.toPageRequest())
    }

    fun log(log: TaskLog) {
        this.taskLogDao.save(log)
    }

    fun getLogger(task: Task? = null): TaskLogger {
        return JdbcTaskLogger(this, task)
    }

    /**
     * 绑定task id 的logger
     */
    class JdbcTaskLogger(private val taskLogService: TaskLogService, private val task: Task?) : TaskLogger {

        override fun log(level: TaskLogger.Level, message: String) {
            taskLogService.log(TaskLog(level, message, task?.id))
        }

    }

}
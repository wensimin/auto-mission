package tech.shali.automission.service

import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import tech.shali.automission.controller.NotFoundException
import tech.shali.automission.dao.TaskDao
import tech.shali.automission.entity.Task
import tech.shali.automission.pojo.TaskQuery
import tech.shali.automission.pojo.TaskSave
import tech.shali.automission.pojo.toClass
import java.util.*

@Service
class TaskService(private val taskDao: TaskDao) {

    fun find(taskQuery: TaskQuery): Page<Task> {
        return taskDao.findAll(taskQuery.toSpecification<Task>(), taskQuery.page.toPageRequest())
    }

    fun save(task: TaskSave) {
        //TODO 启动 任务
        taskDao.save(task.toClass(Task::class))
    }

    fun switchTask(id: String, enabled: Boolean) {
        val task = taskDao.findById(id).orElseThrow()
        task.enabled = enabled
        taskDao.save(task)
        //TODO 启动&停止任务
    }

    fun findOne(id: String): Task {
        return taskDao.findById(id).orElseThrow {
            throw NotFoundException()
        }
    }

}
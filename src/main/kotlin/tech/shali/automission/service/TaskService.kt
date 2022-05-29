package tech.shali.automission.service

import com.github.wensimin.jpaspecplus.findPageBySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.support.CronExpression
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.shali.automission.controller.NotFoundException
import tech.shali.automission.controller.ServiceException
import tech.shali.automission.dao.TaskDao
import tech.shali.automission.entity.Task
import tech.shali.automission.pojo.TaskQuery
import tech.shali.automission.pojo.TaskSave
import tech.shali.automission.pojo.utils.copyTo
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture

/**
 * task service
 * 有状态的service
 * 目前保存了runningTaskMap以及ready状态
 */
@Service
class TaskService(
    private val taskDao: TaskDao,
    taskLogService: TaskLogService,
    // 任务调度器
    // 简单的kv store
    private val taskInstanceService: TaskInstanceService
) {
    /**
     * 正在运行的任务列表
     */
    private val runningTaskMap = ConcurrentHashMap<UUID, ScheduledFuture<*>>()
    private val logger = taskLogService.getLogger()

    @Volatile
    private var ready = false

    fun find(taskQuery: TaskQuery): Page<Task> {
        return taskDao.findPageBySpec(taskQuery, taskQuery.page.toPageRequest())
    }

    /**
     * 保存任务后会自动暂停
     */
    fun save(save: TaskSave): Task {
        val task: Task = save.copyTo()
        validTask(task)
        taskDao.save(task)
        stopTask(task)
        return taskDao.findByIdOrNull(task.id)!!
    }

    /**
     * 检查参数
     */
    private fun validTask(task: Task) {
        if (task.cronExpression?.isNotEmpty() == true && !CronExpression.isValidExpression(task.cronExpression)) {
            throw RuntimeException("cron 表达式后端不支持")
        }
        if (task.interval == null && task.cronExpression == null) {
            throw RuntimeException("没有可执行方式")
        }

    }

    /**
     * 切换状态,任何异常回滚
     */
    @Transactional(rollbackFor = [Exception::class])
    fun switchTask(id: UUID, enabled: Boolean) {
        val task = taskDao.findById(id).orElseThrow()
        task.enabled = enabled
        taskDao.save(task)
        this.switchTask(task)
    }

    fun findOne(id: UUID): Task {
        return taskDao.findById(id).orElseThrow {
            throw NotFoundException()
        }
    }

    /**
     * 切换任务执行状态
     */
    private fun switchTask(task: Task) {
        if (task.enabled) {
            this.startTask(task)
        } else {
            this.stopTask(task)
        }
    }

    /**
     * 启动时会reload task
     */
    @EventListener(ContextRefreshedEvent::class)
    fun init() {
        reloadTask()
    }

    /**
     * 协程异步启动所有任务
     */
    fun reloadTask() {
        ready = false
        // 先停止所有任务
        runningTaskMap.entries.forEach {
            it.value.cancel(true)
        }
        runningTaskMap.clear()
        logger.info("开始初始化所有task")
        val tasks = taskDao.findByEnabled(true)
        runBlocking {
            tasks.forEach {
                launch(Dispatchers.IO) {
                    try {
                        logger.info("${it.name}@${it.id} 正在启动")
                        startTask(it)
                        logger.info("${it.name}@${it.id} 完成")
                    } catch (e: Exception) {
                        logger.error("${it.name}@${it.id} 启动失败 ${e.stackTraceToString()}")
                        logger.warn("${it.name}@${it.id} 启动失败,已经设为停止状态")
                        it.enabled = false
                        taskDao.save(it)
                    }
                }
            }
        }
        logger.info("初始化完毕,目前正在运行的task数量: ${runningTaskMap.size}")
        ready = true
    }


    /**
     * 停止运行任务并且从map种删除
     * @see checkReady
     */
    private fun stopTask(task: Task) {
        this.runningTaskMap[task.id]?.let {
            it.cancel(true)
            this.runningTaskMap.remove(task.id)
            logger.info("${task.id} 任务已停止,当前运行任务数量${runningTaskMap.size}")
        }
    }

    /**
     * 运行任务
     * @see checkReady
     */
    @Synchronized
    private fun startTask(task: Task) {
        //不重复创建task
        if (this.runningTaskMap.containsKey(task.id)) {
            return
        }
        // 存储正在运行的任务map
        runningTaskMap[task.id!!] = taskInstanceService.startScheduleTask(task)
        logger.info("${task.name}@${task.id} 任务启动成功,当前运行任务数量${runningTaskMap.size}")
    }

    /**
     * 外部进行调用前需要check ready
     * 理论上modify都需要check ready状态
     */
    fun checkReady() {
        if (!ready) throw ServiceException(message = "taskService未就绪,稍后再试")
    }


    /**
     * 删除任务
     * @see checkReady
     */
    fun deleteTask(id: UUID) {
        val task = taskDao.findByIdOrNull(id) ?: return
        stopTask(task)
        this.taskDao.delete(task)
    }

    fun startSingle(id: UUID) {
        val task = taskDao.findById(id).orElseThrow()
        this.taskInstanceService.startSingleTask(task)
    }

}
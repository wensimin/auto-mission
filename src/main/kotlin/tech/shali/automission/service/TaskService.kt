package tech.shali.automission.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.support.CronExpression
import org.springframework.scheduling.support.CronTrigger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import tech.shali.automission.controller.NotFoundException
import tech.shali.automission.dao.TaskDao
import tech.shali.automission.entity.Task
import tech.shali.automission.pojo.TaskQuery
import tech.shali.automission.pojo.TaskSave
import tech.shali.automission.pojo.utils.toClass
import java.util.*
import java.util.concurrent.ScheduledFuture
import javax.script.Bindings
import javax.script.Compilable
import javax.script.ScriptEngineManager


@Service
class TaskService(
    private val taskDao: TaskDao,
    private val messageService: MessageService,
    private val objectMapper: ObjectMapper,
    private val taskLogService: TaskLogService,
    // 任务调度器
    private val taskScheduler: TaskScheduler,
    // 简单的kv store
    private val jdbcKVStore: JdbcKVStore
) {
    /**
     * 正在运行的任务列表
     */
    private val runningTaskMap = mutableMapOf<UUID, ScheduledFuture<*>>()
    private val logger = taskLogService.getLogger()
    fun find(taskQuery: TaskQuery): Page<Task> {
        return taskDao.findAll(taskQuery.toSpecification<Task>(), taskQuery.page.toPageRequest())
    }

    /**
     * 保存任务后会自动暂停
     */
    fun save(save: TaskSave) {
        val task = save.toClass(Task::class)
        validTask(task)
        taskDao.save(task)
        stopTask(task)
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
    fun reloadTask() {
        // 先停止所有任务
        runningTaskMap.entries.forEach {
            it.value.cancel(false)
        }
        runningTaskMap.clear()
        logger.info("开始初始化所有task")
        this.taskDao.findByEnabled(true).forEach {
            try {
                startTask(it)
            } catch (e: Exception) {
                logger.warn("${it.id} 启动失败,已经设为停止状态")
                it.enabled = false
                taskDao.save(it)
            }
        }
        logger.info("初始化完毕,目前正在运行的task数量: ${runningTaskMap.size}")

    }

    /**
     * 停止运行任务并且从map种删除
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
     */
    private fun startTask(task: Task) {
        //不重复创建task
        if (this.runningTaskMap.containsKey(task.id)) {
            return
        }
        // 建立task runnable ,用task id 建立logger
        val taskRunnable = this.getTaskRunnable(task.code, taskLogService.getLogger(task.id.toString()))
        val runningTask = when {
            //首先检查cron是否有效
            CronExpression.isValidExpression(task.cronExpression) -> {
                taskScheduler.schedule(taskRunnable, CronTrigger(task.cronExpression!!))
            }
            //然后使用间隔
            task.interval != null -> {
                if (task.async) taskScheduler.scheduleAtFixedRate(
                    taskRunnable,
                    task.interval!!
                ) else
                    taskScheduler.scheduleWithFixedDelay(taskRunnable, task.interval!!)
            }
            else -> {
                throw RuntimeException("没有任何调度方式")
            }
        }
        // 存储正在运行的任务map
        runningTaskMap[task.id!!] = runningTask!!
        logger.info("${task.id} 任务启动成功,当前运行任务数量${runningTaskMap.size}")
    }

    /**
     * 构建task Runnable
     */
    fun getTaskRunnable(code: String, logger: TaskLogger): Runnable {
        val engine = ScriptEngineManager().getEngineByExtension("kts")
        engine as Compilable
        // 编译异常进行抛出
        val compiled = engine.compile(code)
        // api对象持续整个任务周期
        val webClient = WebClient.create()
        val restTemplate = RestTemplate()
        return Runnable {
            try {
                compiled.eval(engine.createBindings().apply {
                    put("logger", logger)
                    putBindings(webClient, restTemplate)
                })
                //运行时的错误进行catch&log
            } catch (e: Exception) {
                logger.error(e.stackTraceToString())
            }
        }
    }


    /**
     * 基本的依赖项注入
     */
    private fun Bindings.putBindings(
        webClient: WebClient = WebClient.create(),
        restTemplate: RestTemplate = RestTemplate()
    ) {
        put("messageService", messageService)
        put("objectMapper", objectMapper)
        put("webClient", webClient)
        put("restTemplate", restTemplate)
        put("store", jdbcKVStore)
    }

    /**
     * 删除任务
     */
    fun deleteTask(id: UUID) {
        val task = taskDao.findByIdOrNull(id) ?: return
        stopTask(task)
        this.taskDao.delete(task)
    }



}
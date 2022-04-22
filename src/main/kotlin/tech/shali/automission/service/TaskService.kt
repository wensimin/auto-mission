package tech.shali.automission.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
import tech.shali.automission.controller.ServiceException
import tech.shali.automission.dao.TaskDao
import tech.shali.automission.entity.Task
import tech.shali.automission.pojo.TaskQuery
import tech.shali.automission.pojo.TaskSave
import tech.shali.automission.pojo.utils.copyTO
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import javax.script.Bindings
import javax.script.Compilable
import javax.script.ScriptEngineManager
import kotlin.concurrent.thread

/**
 * task service
 * 有状态的service
 * 目前保存了runningTaskMap以及ready状态
 */
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
    private val runningTaskMap = ConcurrentHashMap<UUID, ScheduledFuture<*>>()
    private val logger = taskLogService.getLogger()

    @Volatile
    private var ready = false

    fun find(taskQuery: TaskQuery): Page<Task> {
        return taskDao.findAll(taskQuery.toSpecification<Task>(), taskQuery.page.toPageRequest())
    }

    /**
     * 保存任务后会自动暂停
     */
    fun save(save: TaskSave): Task {
        val task = save.copyTO(Task::class)
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
        thread(true, name = "init task thread") {
            runBlocking {
                reloadTask()
            }
        }
    }

    /**
     * 协程异步启动所有任务
     */
    suspend fun reloadTask() {
        ready = false
        withContext(Dispatchers.IO) {
            // 先停止所有任务
            runningTaskMap.entries.forEach {
                it.value.cancel(true)
            }
            runningTaskMap.clear()
            logger.info("开始初始化所有task")
            val tasks = taskDao.findByEnabled(true)
            tasks.map {
                async {
                    try {
                        logger.info("${it.name}@${it.id} 正在启动")
                        startTask(it)
                        logger.info("${it.name}@${it.id} 完成")
                    } catch (e: Exception) {
                        logger.error("${it.name}@${it.id} 启动失败 ${e.stackTraceToString()}")
                        logger.warn("${it.name}@${it.id} 启动失败,已经设为停止状态")
                        it.enabled = false
                        withContext(Dispatchers.IO) {
                            taskDao.save(it)
                        }
                    }
                }
            }.forEach { it.await() }
            logger.info("初始化完毕,目前正在运行的task数量: ${runningTaskMap.size}")
        }
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
    private fun startTask(task: Task) {
        //不重复创建task
        if (this.runningTaskMap.containsKey(task.id)) {
            return
        }
        // 建立task runnable ,用task id 建立logger
        val taskRunnable = this.getTaskRunnable(task.code, taskLogService.getLogger(task))
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
                throw ServiceException(message = "没有任何调度方式")
            }
        }
        // 存储正在运行的任务map
        runningTaskMap[task.id!!] = runningTask!!
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
     * 构建task Runnable
     */
    fun getTaskRunnable(
        code: String,
        logger: TaskLogger
    ): Runnable {
        val engine = ScriptEngineManager().getEngineByExtension("kts")
        engine as Compilable
        // 编译异常进行抛出
        val compiled = engine.compile(code)
        // binging对象持续整个任务周期
        val bindings = engine.createBindings().apply {
            put("logger", logger)
            putBindings()
        }
        return Runnable {
            try {
                compiled.eval(bindings)
                //运行时的错误进行catch&log
            } catch (e: Exception) {
                if (e.cause is InterruptedException) {
                    logger.warn("运行中被中断")
                } else {
                    logger.error(e.stackTraceToString())
                }
            }
        }
    }


    /**
     * 基本的依赖项注入
     */
    private fun Bindings.putBindings(
    ) {
        val webClient = WebClient.create()
        val restTemplate = RestTemplate()
        put("messageService", messageService)
        put("objectMapper", objectMapper)
        put("webClient", webClient)
        put("restTemplate", restTemplate)
        put("store", jdbcKVStore)
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

}
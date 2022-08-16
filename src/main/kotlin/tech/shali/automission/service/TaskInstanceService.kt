package tech.shali.automission.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.support.CronExpression
import org.springframework.scheduling.support.CronTrigger
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import tech.shali.automission.controller.ServiceException
import tech.shali.automission.entity.Task
import tech.shali.automission.pojo.TaskInstance
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.script.Bindings
import javax.script.Compilable
import javax.script.ScriptEngineManager


/**
 * 任务实例管理service
 * 对scheduler进行了一次包装
 * 启动任务时存储了kv map 以及list进行任务实例的管理
 * 本service不提供schedule任务的停止,仅提供一次性任务的停止&启动,所有任务的启动
 */
@Service
class TaskInstanceService(
    // 任务调度器
    private val taskScheduler: TaskScheduler,
    private val messageService: MessageService,
    private val objectMapper: ObjectMapper,
    private val taskLogService: TaskLogService,
    // 简单的kv store
    private val jdbcKVStore: JdbcKVStore
) {
    /**
     * 运行中的taskMap
     */
    private val instanceMap = ConcurrentHashMap<String, TaskInstance>()
    private val instanceList = Collections.synchronizedList(LinkedList<TaskInstance>())

    companion object {
        const val TIMEOUT = 10L
    }

    /**
     * 启动一个周期性任务
     * 如果任务没有足够的调度信息则抛出业务异常
     */
    fun startScheduleTask(task: Task): ScheduledFuture<*> {
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
                throw ServiceException(message = "没有任何周期调度方式")
            }
        }
        addTask(task.name, task.code, true, runningTask!!)
        return runningTask
    }

    /**
     * 启动一次性任务
     */
    fun startSingleTask(task: Task) {
        val taskRunnable = this.getTaskRunnable(task.code, taskLogService.getLogger(task))
        addTask(task.name, task.code, false, taskScheduler.schedule(taskRunnable, Instant.now()))
    }

    /**
     * 启动debug任务,debug任务仅需要提供代码以及自定义logger
     */
    fun startDebugTask(code: String, logger: TaskLogger): ScheduledFuture<*> {
        return startDebugTask(code, this.getTaskRunnable(code, logger))
    }

    /**
     * debug任务重载，可以自定义runnable
     */
    fun startDebugTask(code: String, runnable: Runnable): ScheduledFuture<*> {
        val runningTask = taskScheduler.schedule(runnable, Instant.now())
        addTask(null, code, false, runningTask)
        return runningTask
    }

    /**
     * 将启动的任务保留到map&list中
     */
    private fun addTask(name: String?, code: String, schedule: Boolean, task: ScheduledFuture<*>) {
        val key = UUID.randomUUID().toString()
        val instance = TaskInstance(key, code, schedule, task, name)
        instanceMap[key] = instance
        instanceList.add(instance)
    }

    /**
     * 查询list中的任务
     */
    fun findTask(done: Boolean?): List<TaskInstance> {
        return instanceList.filter { it.done == (done ?: it.done) }
    }

    /**
     * 停止指定key的任务
     * 仅非周期任务可关闭
     */
    fun stopTask(key: String) {
        instanceMap[key]?.let {
            if (it.schedule) throw ServiceException(message = "实例管理不允许关闭周期性任务")
            it.task.cancel(true)
        }
    }

    /**
     * 清理已经结束的task
     */
    fun clearDoneTask() {
        this.instanceList.removeIf { it.done }
        this.instanceMap.forEach { if (it.value.done) instanceMap.remove(it.key) }
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
        val webClient = createWebClient()
        val restTemplate = createRestTemplate()
        put("messageService", messageService)
        put("objectMapper", objectMapper)
        put("webClient", webClient)
        put("restTemplate", restTemplate)
        put("store", jdbcKVStore)
    }

    private fun createWebClient(): WebClient {
        return WebClient.builder().clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create().apply {
                    responseTimeout(Duration.ofSeconds(TIMEOUT))
                    option(ChannelOption.CONNECT_TIMEOUT_MILLIS, TIMEOUT.toInt() * 1000)
                    doOnConnected { conn ->
                        conn
                            .addHandler(ReadTimeoutHandler(TIMEOUT, TimeUnit.SECONDS))
                            .addHandler(WriteTimeoutHandler(TIMEOUT.toInt()))
                    }
                }
            )
        ).build()
    }

    private fun createRestTemplate(): RestTemplate {
        return RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(TIMEOUT))
            .setReadTimeout(Duration.ofSeconds(TIMEOUT))
            .build()
    }

}



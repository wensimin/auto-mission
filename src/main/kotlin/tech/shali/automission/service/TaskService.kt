package tech.shali.automission.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.Page
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.support.CronExpression
import org.springframework.scheduling.support.CronTrigger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import tech.shali.automission.controller.NotFoundException
import tech.shali.automission.dao.TaskDao
import tech.shali.automission.entity.Task
import tech.shali.automission.pojo.DebugCodeVo
import tech.shali.automission.pojo.TaskQuery
import tech.shali.automission.pojo.TaskSave
import tech.shali.automission.pojo.utils.toClass
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
    private val taskScheduler: TaskScheduler
) {
    /**
     * 正在运行的任务列表
     */
    private val runningTaskMap = mutableMapOf<String, ScheduledFuture<*>>()

    fun find(taskQuery: TaskQuery): Page<Task> {
        return taskDao.findAll(taskQuery.toSpecification<Task>(), taskQuery.page.toPageRequest())
    }

    fun save(task: TaskSave) {
        taskDao.save(task.toClass(Task::class))
    }

    @Transactional
    fun switchTask(id: String, enabled: Boolean) {
        val task = taskDao.findById(id).orElseThrow()
        task.enabled = enabled
        taskDao.save(task)
        this.switchTask(task)
    }

    fun findOne(id: String): Task {
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
     * 停止运行任务并且从map种删除
     */
    private fun stopTask(task: Task) {
        this.runningTaskMap[task.id]?.cancel(false)
        this.runningTaskMap.remove(task.id)
    }

    /**
     * 运行任务
     */
    private fun startTask(task: Task) {
        // 建立task runnable ,用task id 建立logger
        val taskRunnable = this.getTaskRunnable(task.code, taskLogService.getLogger(task.id))
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
        runningTaskMap[task.id] = runningTask!!
    }

    /**
     * 构建task Runnable
     */
    private fun getTaskRunnable(code: String, logger: TaskLogger): Runnable {
        val engine = ScriptEngineManager().getEngineByExtension("kts")
        engine as Compilable
        val compiled = engine.compile(code)
        //编译异常进行抛出
        return Runnable {
            try {
                compiled.eval(engine.createBindings().apply {
                    put("logger", logger)
                    putBindings()
                })
                //运行时的错误进行catch&log
            } catch (e: Exception) {
                logger.error(e.stackTraceToString())
            }
        }
    }

    /**
     * 测试运行一次代码
     */
    fun testCode(code: DebugCodeVo): String {
        //代码试运行时的logger
        val logger = DebugTaskLogger()
        try {
            getTaskRunnable(code.code!!, logger).run()
        } catch (e: Exception) {
            logger.error(e.stackTraceToString())
        }
        return logger.view()
    }


    /**
     * 基本的依赖项注入
     */
    private fun Bindings.putBindings() {
        put("messageService", messageService)
        put("objectMapper", objectMapper)
        put("webClient", WebClient.create())
    }

}
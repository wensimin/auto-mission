package tech.shali.automission.service

import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import tech.shali.automission.pojo.DebugCodeVo
import tech.shali.automission.pojo.DebugResult
import java.util.*
import java.util.concurrent.ScheduledFuture

/**
 * 提供前端预运行代码的流程服务
 */
@Service
class DebugService(
    private val taskService: TaskService,
    private val taskScheduler: TaskScheduler,
) {
    private val debuggingTask = mutableMapOf<String, DebugSchedule>()

    companion object {
        //debug任务最多能执行且保存数据的时间 单位为分钟
        private const val TIMEOUT = 10
    }

    fun debugCode(code: DebugCodeVo): DebugResult {
        val id = UUID.randomUUID().toString()
        val logger = DebugTaskLogger()
        val res = DebugResult(id)
        try {
            val task = taskService.getTaskRunnable(code.code!!, logger)
            val schedule = taskScheduler.schedule(task, Date())
            debuggingTask[id] = DebugSchedule(schedule, logger)
            logger.debug("已经启动debug")
            // 启动一个进程去进行超时关闭
            Thread {
                Thread.sleep(TIMEOUT * 60 * 1000L)
                stopTask(id)
            }.start()
        } catch (e: Exception) {
            logger.error("启动测试失败")
            logger.error(e.stackTraceToString())
            res.end = true
        }
        res.consoleText = logger.view()
        return res
    }

    fun viewResult(id: String): DebugResult? {
        return debuggingTask[id]?.let {
            DebugResult(id, it.schedule.isDone, it.logger.view())
        }
    }

    fun stopDebug(id: String): DebugResult? {
        return debuggingTask[id]?.let {
            this.stopTask(id)
            DebugResult(id, true, it.logger.view())
        }
    }

    private fun stopTask(id: String) {
        this.debuggingTask[id]?.let {
            it.schedule.cancel(true)
            this.debuggingTask.remove(id)
            it.logger.warn("$id debug停止")
        }
    }

    class DebugSchedule(val schedule: ScheduledFuture<*>, val logger: DebugTaskLogger)
}
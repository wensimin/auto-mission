package tech.shali.automission.service

import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import tech.shali.automission.controller.ErrorType
import tech.shali.automission.controller.ServiceException
import tech.shali.automission.pojo.DebugCodeVo
import tech.shali.automission.pojo.DebugResult
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture

/**
 * 提供前端预运行代码的流程服务
 */
@Service
class DebugService(
    private val taskService: TaskService,
    private val taskScheduler: TaskScheduler,
    taskLogService: TaskLogService
) {
    private val debuggingTask = ConcurrentHashMap<String, DebugSchedule>()
    private val taskLogger = taskLogService.getLogger()

    companion object {
        // debug任务最多能执行且保存数据的时间 单位为分钟
        private const val TIMEOUT = 10

        // 同时允许测试任务运行的数量
        private const val DEBUG_LIMIT = 5
    }

    fun debugCode(code: DebugCodeVo): DebugResult {
        if (debuggingTask.size > DEBUG_LIMIT) {
            throw ServiceException(ErrorType.DEBUG_LIMIT)
        }
        val id = UUID.randomUUID().toString()
        val logger = DebugTaskLogger()
        val res = DebugResult(id)
        try {
            val task = taskService.getTaskRunnable(code.code!!, logger)
            val schedule = taskScheduler.schedule(task, Date())
            debuggingTask[id] = DebugSchedule(schedule, logger, System.currentTimeMillis())
            logger.debug("已经启动debug")
        } catch (e: Exception) {
            logger.error("启动测试失败")
            logger.error(e.stackTraceToString())
            res.end = true
        }
        res.consoleText = logger.view()
        return res
    }

    /**
     * 每分钟清理一次task
     */
    @Scheduled(cron = "0 * * * * *")
    fun clearTask() {
        taskLogger.debug("扫描正在debug的任务列表 count: ${debuggingTask.size}")
        debuggingTask.filter {
            // 超时时间
            val endTime = it.value.startTime + TIMEOUT * 60 * 1000
            // 超时或结束了的任务
            endTime > System.currentTimeMillis() || it.value.schedule.isDone
        }.forEach {
            taskLogger.debug("销毁debug的任务 启动于: ${it.value.startTime}")
            this.stopTask(it.key)
        }
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
            it.logger.warn("任务被停止")
        }
    }

    class DebugSchedule(val schedule: ScheduledFuture<*>, val logger: DebugTaskLogger, val startTime: Long)
}
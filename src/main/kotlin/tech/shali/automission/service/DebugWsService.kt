package tech.shali.automission.service

import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture

@Service
class DebugWsService(
    private val taskInstanceService: TaskInstanceService,
    taskLogService: TaskLogService
) {
    private val debugMap = ConcurrentHashMap<String, ScheduledFuture<*>>()
    private val systemLogger = taskLogService.getLogger()
    fun stopTask(id: String) {
        debugMap[id]?.let {
            it.cancel(true)
            debugMap.remove(id)
            systemLogger.info("clear debug task ws sessionId: $id")
        }
    }

    fun startTask(code: String, logger: WSDebugTaskLogger, id: String) {
        if (debugMap[id] != null) {
            systemLogger.warn("session 已经有在执行的task 忽略后续message")
            return
        }
        systemLogger.info("start debug task ws sessionId: $id")
        val runnable = taskInstanceService.getTaskRunnable(code, logger)
        val schedule = taskInstanceService.startDebugTask(code) {
            runnable.run()
            logger.complete()
        }
        debugMap[id] = schedule
    }


}
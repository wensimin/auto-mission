package tech.shali.automission.service

import org.springframework.stereotype.Service
import java.security.Principal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture

@Service
class DebugWsService(
    private val taskInstanceService: TaskInstanceService,
    taskLogService: TaskLogService
) {
    private val debugMap = ConcurrentHashMap<String, ScheduledFuture<*>>()
    private val tokenMap = ConcurrentHashMap<String, Pair<Instant, Principal>>()

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

    /**
     * 生成用于访问ws的token
     * js在建立ws连接时不允许携带auth header
     * 所以先通过http连接确认授权,生成token,后续使用这个token进行ws握手
     */
    fun generateToken(principal: Principal): String {
        val token = UUID.randomUUID().toString()
        tokenMap[token] = Instant.now().plus(5L, ChronoUnit.MINUTES) to principal
        clearToken()
        return token
    }

    /**
     * 检查token是否有效
     */
    fun checkToken(token: String): Boolean = getPrincipal(token) != null

    /**
     * 由token获取当前认证
     */
    fun getPrincipal(token: String): Principal? {
        clearToken()
        return tokenMap[token]?.second
    }

    /**
     * 删除过期token
     */
    private fun clearToken() {
        tokenMap
            .filterValues { pair -> pair.first.isBefore(Instant.now()) }
            .keys.forEach { tokenMap.remove(it) }
    }


}
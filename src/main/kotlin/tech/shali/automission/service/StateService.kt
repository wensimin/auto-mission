package tech.shali.automission.service

import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Service
import tech.shali.automission.pojo.SystemState
import java.lang.management.ManagementFactory

@Service
class StateService(private val taskScheduler: TaskScheduler) {

    fun state(): SystemState {
        val totalMemory = Runtime.getRuntime().totalMemory()
        val freeMemory = Runtime.getRuntime().freeMemory()
        val memory = (totalMemory - freeMemory)
        val threadCount = ManagementFactory.getThreadMXBean().threadCount
        taskScheduler as ThreadPoolTaskScheduler
        val activeCount = taskScheduler.activeCount
        val taskMaxWorker = taskScheduler.scheduledThreadPoolExecutor.corePoolSize
        return SystemState(activeCount, taskMaxWorker, memory, totalMemory, threadCount)
    }
}
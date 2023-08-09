package tech.shali.automission.pojo

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class TaskInstance(
    val key: String,
    val code: String,
    val schedule: Boolean,
    @JsonIgnore
    val task: ScheduledFuture<*>,
    val name: String?,
    val createDate: Date = Date(),
) {
    val done: Boolean
        get() = task.isDone
    val delayMessage: String
        get() {
            val sec = task.getDelay(TimeUnit.SECONDS)
            val suffix = if (sec > 0) "后" else "前"
            val secAbs = abs(sec)
            val value = if (secAbs < 60) "$secAbs 秒" else "${secAbs / 60} 分"
            return value + suffix
        }
    val running: Boolean
        // 可从delay时间判断任务是否正在运行
        get() = task.getDelay(TimeUnit.MILLISECONDS) <= 0

    /**
     * 停止任务
     */
    fun stop() {
        task.cancel(true)
    }
}
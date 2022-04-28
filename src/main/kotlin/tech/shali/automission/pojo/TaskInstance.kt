package tech.shali.automission.pojo

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*
import java.util.concurrent.ScheduledFuture

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
}
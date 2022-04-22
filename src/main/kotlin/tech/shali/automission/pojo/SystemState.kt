package tech.shali.automission.pojo

data class SystemState(
    val runningTaskCount: Int,
    val taskWorker: Int,
    val taskMaxWorker: Int,
    val memoryUsage: Long,
    val totalMemory: Long,
    val maxMemory: Long,
    val threadCount: Int
)

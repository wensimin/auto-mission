package tech.shali.automission.pojo

import java.time.Instant

data class TaskLogQuery(
    val label: String? = null,
    val text: String? = null,
    val taskId: String? = null,
    val startDate: Instant? = null,
    val endDate: Instant? = null,
    val page: PageVo = PageVo()
)

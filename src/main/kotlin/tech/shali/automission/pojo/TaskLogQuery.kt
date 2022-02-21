package tech.shali.automission.pojo

import tech.shali.automission.entity.utils.*
import java.time.Instant

data class TaskLogQuery(
    val label: String? = null,
    @Like(Like.Type.ALL)
    val text: String? = null,
    val taskId: String? = null,
    @Greater("createDate")
    val startDate: Instant? = null,
    @Less("createDate")
    val endDate: Instant? = null,
    @Ignore
    val page: PageVo = PageVo()
) : QueryParam

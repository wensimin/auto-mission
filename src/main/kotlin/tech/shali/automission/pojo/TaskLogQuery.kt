package tech.shali.automission.pojo

import tech.shali.automission.entity.utils.*
import java.util.*

data class TaskLogQuery(
    @Eq(igCase = true)
    val label: String? = null,
    @Like(Like.Type.ALL)
    val text: String? = null,
    val taskId: String? = null,
    @Greater("createDate")
    val startDate: Date? = null,
    @Less("createDate")
    val endDate: Date? = null,
    @Ignore
    val page: PageVo = PageVo()
) : QueryParam

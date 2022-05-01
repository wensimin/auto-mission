package tech.shali.automission.pojo

import com.github.wensimin.jpaspecplus.Ignore
import com.github.wensimin.jpaspecplus.Join
import com.github.wensimin.jpaspecplus.JoinPath
import com.github.wensimin.jpaspecplus.specification.Greater
import com.github.wensimin.jpaspecplus.specification.Less
import com.github.wensimin.jpaspecplus.specification.Like
import tech.shali.automission.service.TaskLogger
import java.util.*

@Join("task")
data class TaskLogQuery(
    @Less("level")
    val level: TaskLogger.Level? = null,
    @Like(Like.Type.ALL, separator = " ")
    val text: String? = null,
    val taskId: UUID? = null,
    @JoinPath("task")
    @Like(fieldName = "name", type = Like.Type.ALL)
    val taskName: String? = null,
    @Greater("createDate")
    val startDate: Date? = null,
    @Less("createDate")
    val endDate: Date? = null,
    @Ignore
    val page: PageVo = PageVo()
)

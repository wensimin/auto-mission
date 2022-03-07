package tech.shali.automission.pojo

import tech.shali.automission.entity.utils.Ignore
import tech.shali.automission.entity.utils.Like
import tech.shali.automission.entity.utils.QueryParam

data class TaskQuery(
    @Like(Like.Type.ALL)
    var name: String? = null,
    @Like(Like.Type.ALL)
    var description: String? = null,
    var enabled: Boolean? = null,
    @Ignore
    val page: PageVo = PageVo()
) : QueryParam
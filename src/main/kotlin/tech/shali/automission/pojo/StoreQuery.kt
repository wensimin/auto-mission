package tech.shali.automission.pojo

import tech.shali.automission.entity.utils.Ignore
import tech.shali.automission.entity.utils.Like
import tech.shali.automission.entity.utils.QueryParam

data class StoreQuery(
    @Like(Like.Type.ALL)
    val key: String? = null,
    @Ignore
    val page: PageVo = PageVo()
) : QueryParam

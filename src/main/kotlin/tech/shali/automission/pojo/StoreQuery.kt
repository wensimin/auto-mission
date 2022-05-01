package tech.shali.automission.pojo

import com.github.wensimin.jpaspecplus.Ignore
import com.github.wensimin.jpaspecplus.specification.Like


data class StoreQuery(
    @Like(Like.Type.ALL)
    val key: String? = null,
    @Ignore
    val page: PageVo = PageVo()
)

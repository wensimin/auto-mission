package tech.shali.automission.pojo

import com.github.wensimin.jpaspecplus.Ignore
import com.github.wensimin.jpaspecplus.specification.Like


data class TaskQuery(
    @Like(Like.Type.ALL)
    var name: String? = null,
    @Like(Like.Type.ALL)
    var description: String? = null,
    var enabled: Boolean? = null,
    @Ignore
    val page: PageVo = PageVo()
)
package tech.shali.automission.pojo

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

/**
 * 用于分页参数
 */
data class PageVo(
    var number: Int = 0,
    var size: Int = 20,
    var direction: List<String> = listOf()
) {
    fun toPageRequest(): PageRequest {
        return if (direction.isEmpty()) {
            PageRequest.of(number, size)
        } else {
            val orders = direction.map {
                it.split(" ").let { sort ->
                    //  first为属性名,  last为排序方式
                    Sort.Order(Sort.Direction.valueOf(sort.last().uppercase()), sort.first())
                }
            }
            PageRequest.of(number, size, Sort.by(orders))
        }
    }
}
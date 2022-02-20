package tech.shali.automission.pojo

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

/**
 * 用于分页参数
 */
data class PageVo(
    var number: Int = 0,
    var size: Int = 20,
    var direction: Sort.Direction = Sort.Direction.ASC,
    var properties: Set<String> = emptySet()
) {
    fun toPageRequest(): PageRequest {
        return if (properties.isEmpty()) {
            PageRequest.of(number, size)
        } else {
            PageRequest.of(number, size, direction, *properties.toTypedArray())
        }
    }
}
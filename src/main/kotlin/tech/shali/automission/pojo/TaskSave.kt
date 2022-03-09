package tech.shali.automission.pojo

import org.hibernate.validator.constraints.Length
import java.util.*
import javax.validation.constraints.NotEmpty

/**
 * Task 参数类
 */
data class TaskSave(
    var id: UUID? = null,
    @field:NotEmpty
    var name: String?,
    @Length(max = 255)
    @field:NotEmpty
    var description: String?,
    @field:NotEmpty
    var code: String?,
    var cronExpression: String? = null,
    var interval: Long? = null,
    var async: Boolean = false
)

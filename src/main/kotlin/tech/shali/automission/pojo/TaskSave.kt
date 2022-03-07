package tech.shali.automission.pojo

import org.hibernate.validator.constraints.Length
import java.util.*
import javax.validation.constraints.NotEmpty

data class TaskSave(
    var id: String = UUID.randomUUID().toString(),
    @field:NotEmpty
    var name: String?,
    @Length(max = 255)
    @field:NotEmpty
    var description: String?,
    @field:NotEmpty
    var code: String?,
    @field:NotEmpty
    var cronExpression: String?,
    var enabled: Boolean = false
)

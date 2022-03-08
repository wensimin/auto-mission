package tech.shali.automission.pojo

import javax.validation.constraints.NotEmpty

class DebugCodeVo(
    @NotEmpty
    val code: String?
)
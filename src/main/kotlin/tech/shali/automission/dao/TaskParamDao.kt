package tech.shali.automission.dao

import org.springframework.data.jpa.repository.JpaRepository
import tech.shali.automission.entity.TaskParam

interface TaskParamDao : JpaRepository<TaskParam, String> {
    fun findByKey(accessKey: String):TaskParam
}
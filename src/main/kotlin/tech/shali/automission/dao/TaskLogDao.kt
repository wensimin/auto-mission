package tech.shali.automission.dao

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import tech.shali.automission.entity.TaskLog
import java.util.*

interface TaskLogDao : JpaRepository<TaskLog, UUID>, JpaSpecificationExecutor<TaskLog> {
}
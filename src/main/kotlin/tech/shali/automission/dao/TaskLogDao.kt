package tech.shali.automission.dao

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import tech.shali.automission.entity.TaskLog

interface TaskLogDao : JpaRepository<TaskLog, String>, JpaSpecificationExecutor<TaskLog> {
}
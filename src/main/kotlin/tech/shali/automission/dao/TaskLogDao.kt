package tech.shali.automission.dao

import org.springframework.data.jpa.repository.JpaRepository
import tech.shali.automission.entity.TaskLog

interface TaskLogDao : JpaRepository<TaskLog, String>
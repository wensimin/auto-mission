package tech.shali.automission.dao

import org.springframework.data.jpa.repository.JpaRepository
import tech.shali.automission.entity.Task

interface TaskDao : JpaRepository<Task, String>
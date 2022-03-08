package tech.shali.automission.dao

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import tech.shali.automission.entity.Task

interface TaskDao : JpaRepository<Task, String>, JpaSpecificationExecutor<Task> {

    fun findByEnabled(enable: Boolean): List<Task>
}
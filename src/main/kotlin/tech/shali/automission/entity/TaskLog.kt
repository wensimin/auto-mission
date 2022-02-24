package tech.shali.automission.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.validation.constraints.Max

@Entity
class TaskLog(
    @Column(nullable = false)
    val label: String,
    @Column(nullable = false)
    @Max(255)
    val text: String,
    val taskId: String? = null,
) : Data()
package tech.shali.automission.entity

import javax.persistence.Column
import javax.persistence.Entity

@Entity
class TaskLog(
    @Column(nullable = false)
    val label: String,
    @Column(nullable = false)
    val text: String,
    @Column(nullable = false)
    val taskId: String,
) : Data()
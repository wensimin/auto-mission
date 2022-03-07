package tech.shali.automission.entity

import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class TaskLog(
    @Column(nullable = false)
    val label: String,
    @Column(nullable = false)
    val text: String,
    val taskId: String? = null,
    @Id @Column(nullable = false)
    var id: String = UUID.randomUUID().toString()
) : Data()
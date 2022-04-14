package tech.shali.automission.entity

import org.hibernate.annotations.Type
import tech.shali.automission.service.TaskLogger
import java.util.*
import javax.persistence.*

@Entity
class TaskLog(
    @Column(nullable = false)
    @Enumerated
    val level: TaskLogger.Level,
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(nullable = false)
    val text: String,
    @Column(name = "task_id")
    val taskId: UUID? = null,
    @ManyToOne
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    val task: SimpleTask? = null,
    @Id
    @GeneratedValue
    var id: UUID? = null
) : Data()
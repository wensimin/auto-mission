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
    val taskId: String? = null,
    @Id
    @GeneratedValue
    var id: UUID? = null
) : Data()
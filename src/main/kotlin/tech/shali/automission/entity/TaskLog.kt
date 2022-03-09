package tech.shali.automission.entity

import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.*

@Entity
class TaskLog(
    @Column(nullable = false)
    val label: String,
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(nullable = false)
    val text: String,
    val taskId: String? = null,
    @Id
    @GeneratedValue
    var id: UUID? = null
) : Data()
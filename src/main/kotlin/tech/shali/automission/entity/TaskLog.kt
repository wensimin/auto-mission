package tech.shali.automission.entity

import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob

@Entity
class TaskLog(
    @Column(nullable = false)
    val label: String,
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(nullable = false)
    val text: String,
    val taskId: String? = null,
    @Id @Column(nullable = false)
    var id: String = UUID.randomUUID().toString()
) : Data()
package tech.shali.automission.entity

import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob

@Entity
class TaskParam(
    @Id
    val key: String,
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    var value: String,
    var remark: String
)
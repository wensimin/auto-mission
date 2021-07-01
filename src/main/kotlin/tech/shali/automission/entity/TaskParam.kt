package tech.shali.automission.entity

import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class TaskParam(
    @Id
    var id: String = UUID.randomUUID().toString(),
    @Column(nullable = false)
    val key: String,
    var value: String,
    var remark: String
)
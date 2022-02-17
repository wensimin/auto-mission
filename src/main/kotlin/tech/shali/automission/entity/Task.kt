package tech.shali.automission.entity

import org.hibernate.annotations.Type
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Lob

@Entity
class Task(
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false)
    var description: String,
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(nullable = false)
    var code: String,
    @Column(nullable = false)
    var cronExpression: String,
    @Column(nullable = false)
    var enabled: Boolean = false
) : Data()
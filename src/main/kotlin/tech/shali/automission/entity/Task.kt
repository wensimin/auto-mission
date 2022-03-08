package tech.shali.automission.entity

import org.hibernate.annotations.Type
import org.hibernate.validator.constraints.Length
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob

@Entity
class Task(
    @Id @Column(nullable = false)
    var id: String,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false)
    @Length(max = 255)
    var description: String,
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(nullable = false)
    var code: String,
    var cronExpression: String? = null,
    var interval: Long? = null,
    var async: Boolean = false,
    @Column(nullable = false)
    var enabled: Boolean = false
) : Data()
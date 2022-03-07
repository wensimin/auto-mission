package tech.shali.automission.entity

import org.hibernate.annotations.Type
import org.hibernate.validator.constraints.Length
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob
import javax.validation.constraints.NotEmpty

@Entity
class Task(
    @Id @Column(nullable = false)
    var id: String,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false)
    @Length(max = 255)
    @NotEmpty
    var description: String,
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(nullable = false)
    @NotEmpty
    var code: String,
    @Column(nullable = false)
    @NotEmpty
    var cronExpression: String,
    @Column(nullable = false)
    var enabled: Boolean = false
) : Data()
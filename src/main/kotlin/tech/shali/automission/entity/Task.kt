package tech.shali.automission.entity

import org.hibernate.annotations.Type
import org.hibernate.validator.constraints.Length
import java.util.*
import javax.persistence.*

@Entity
class Task(
    @Id
    @GeneratedValue
    var id: UUID? = null,
    @Column(nullable = false)
    var name: String,
    /**
     * 描述
     */
    @Column(nullable = false)
    @Length(max = 255)
    var description: String,
    /**
     * 代码
     */
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

@Entity
@Table(name = "task")
class SimpleTask(
    @Id
    var id: UUID? = null,
    var name: String
)
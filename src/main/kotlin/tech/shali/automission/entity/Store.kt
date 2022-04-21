package tech.shali.automission.entity

import org.hibernate.annotations.Type
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob
import javax.validation.constraints.NotEmpty

/**
 * 简单的kv store
 */
@Entity
class Store(
    @Id
    @field:NotEmpty
    val key: String,
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    val value: String?
)
package tech.shali.automission.pojo

import javax.persistence.Entity
import javax.persistence.Id
import javax.validation.constraints.NotEmpty

/**
 * 简单的kv store
 */
@Entity
class Store(
    @Id
    @field:NotEmpty
    val key: String,
    val value: String?
)
package tech.shali.automission.pojo

import javax.persistence.Entity
import javax.persistence.Id

/**
 * 简单的kv store
 */
@Entity
class Store(
    @Id
    val key: String,
    val value: String?
)
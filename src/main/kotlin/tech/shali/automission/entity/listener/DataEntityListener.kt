package tech.shali.automission.entity.listener

import tech.shali.automission.entity.Data
import javax.persistence.PreUpdate

class DataEntityListener {
    @PreUpdate
    fun methodExecuteBeforeUpdate(reference: Data) {
        reference.beforeUpdate()
    }
}

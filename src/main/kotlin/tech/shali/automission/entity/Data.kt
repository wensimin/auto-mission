package tech.shali.automission.entity

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY
import tech.shali.automission.entity.listener.DataEntityListener
import java.time.Instant
import java.util.*
import javax.persistence.Column
import javax.persistence.EntityListeners
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
@EntityListeners(DataEntityListener::class)
open class Data(
    @Id @Column(nullable = false) @JsonProperty(access = READ_ONLY) open var id: String = UUID.randomUUID().toString(),
    @Column(nullable = false, updatable = false) @JsonProperty(access = READ_ONLY) var createDate: Date = Date(),
    @Column(nullable = false) @JsonProperty(access = READ_ONLY) var updateDate: Date = Date()
) {

    fun beforeUpdate() {
        updateDate = Date()
    }
}
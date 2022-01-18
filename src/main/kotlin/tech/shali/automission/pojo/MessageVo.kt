package tech.shali.automission.pojo

import java.io.Serializable

/**
 * message对象
 * mq 中使用该对象json string
 */
data class MessageVo(
    var title: String?,
    var body: String?,
    var toUser: String? = null,
    var toTopic: String? = null,
    var fromClient: String? = null,
    var url: String? = null
) : Serializable
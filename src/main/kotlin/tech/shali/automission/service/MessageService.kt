package tech.shali.automission.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Service
import tech.shali.automission.pojo.MessageVo

@Service
class MessageService(
    private val rabbitTemplate: RabbitTemplate,
    private val mailSender: JavaMailSender,
    private val objectMapper: ObjectMapper
) {
    //HARD CODE 消息队列key
    private val routingKey = "message"

    // 自发自收邮件
    private val noticeMail = (mailSender as JavaMailSenderImpl).username!!

    fun sendMessage(message: MessageVo) {
        rabbitTemplate.convertAndSend(
            routingKey,
            objectMapper.writeValueAsString(message)
        )
    }

    /**
     * 对指定用户发送message
     */
    fun sendMessageToUser(
        title: String,
        body: String,
        user: String,
        url: String? = null,
        fromClient: String? = "autoTask",
        priority: MessageVo.Priority = MessageVo.Priority.NORMAL
    ) {
        this.sendMessage(
            MessageVo(
                title = title,
                body = body,
                toUser = user,
                url = url,
                fromClient = fromClient,
                priority = priority
            )
        )
    }

    /**
     * 对一个主题发送message
     */
    fun sendMessageToTopic(
        title: String,
        body: String,
        topic: String,
        url: String? = null,
        fromClient: String? = "autoTask",
        priority: MessageVo.Priority = MessageVo.Priority.NORMAL
    ) {
        this.sendMessage(
            MessageVo(
                title = title,
                body = body,
                toTopic = topic,
                url = url,
                fromClient = fromClient,
                priority = priority
            )
        )
    }

    /**
     * 对管理员发送邮件
     */
    fun sendMail(title: String, body: String) {
        mailSender.send(SimpleMailMessage().apply {
            setFrom(noticeMail)
            setTo(noticeMail)
            setSubject(title)
            setText(body)
        })
    }
}
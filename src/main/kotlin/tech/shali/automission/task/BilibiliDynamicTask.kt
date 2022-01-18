package tech.shali.automission.task

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import tech.shali.automission.dao.TaskParamDao
import tech.shali.automission.pojo.MessageVo

@Component
class BilibiliDynamicTask(
    private val taskParamDao: TaskParamDao,
    private val objectMapper: ObjectMapper,
    private val rabbitTemplate: RabbitTemplate
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val robotUid: String = taskParamDao.findByKey(idKey).value

    companion object {
        private const val bellaTopic = "bella-dynamic"
        private const val bellaUid = 672353429
        private const val clientId = "autoTask"
        private const val routingKey = "message"
        private const val cookieKey = "bilibili-robot-web-cookie"
        private const val idKey = "bilibili-robot-uid"
        private const val videoType = 8
        private val textType = listOf(1, 2, 4)
        private const val cyclicUrl =
            "https://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/web_cyclic_num?type_list=268435455"

        // uid为 %s 通过format
        private const val readNewUrl =
            "https://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/dynamic_new?uid=%s&type_list=268435455&from=weball&platform=web"
    }

    /**
     * bella
     */
    @Scheduled(initialDelay = 5000, fixedDelay = 5000)
    fun bellaDynamic() {
        var errorMessage: String? = null
        try {
            val cookie = taskParamDao.findByKey(cookieKey)
            val cyclicRes = WebClient.create().get().uri(cyclicUrl)
                .header("cookie", cookie.value)
                .retrieve()
                .bodyToMono(JsonNode::class.java)
                .block()
            cyclicRes?.get("code")?.let {
                errorMessage = "code ${it.intValue()} message: ${cyclicRes["message"].textValue()}"
            }
            val newNum = cyclicRes?.get("data")?.get("update_num")?.asInt()!!
            if (newNum == 0) return
            logger.info("newNum:$newNum")
            WebClient.create().get()
                .uri(String.format(readNewUrl, robotUid))
                .header("cookie", cookie.value)
                .retrieve()
                .bodyToMono(JsonNode::class.java)
                .block()
                .also {
                    receiveMessage(it!!)
                }

        } catch (e: Exception) {
            //error目前使用log
            logger.warn(errorMessage ?: e::class.java.name)
        }
    }

    private fun receiveMessage(node: JsonNode) {
        val newNum = node["data"]["new_num"].asInt()
        val cards = node["data"]["cards"].toList().subList(0, newNum)
        cards.forEach {
            //对比id仅匹配发送
            val uId = it["desc"]["uid"].intValue()
            if (uId != bellaUid) {
                return
            }
            val type = it["desc"]["type"].intValue()
            val messageVo: MessageVo = when {
                type == videoType -> {
                    createVideoVo(it)
                }
                textType.contains(type) -> {
                    createTextVo(it)
                }
                else -> {
                    createOtherVo(type)
                }
            }
            logger.info("send message $messageVo")
            sendMessage(messageVo)
        }
    }

    private fun createOtherVo(typeid: Int): MessageVo {
        return MessageVo(
            title = "uid $bellaUid 发动态了",
            body = "动态为不支持的类型$typeid",
            toTopic = bellaTopic,
            fromClient = clientId
        )
    }

    private fun createTextVo(it: JsonNode): MessageVo {
        val dynamicId = it["desc"]["dynamic_id"]
        val cardJson = it["card"].asText()
        val card: JsonNode = objectMapper.readValue(cardJson)
        val user = card["user"]
        val name = user["uname"] ?: user["name"]
        val body = card["item"].let { item ->
            item["content"] ?: item["description"]
        }.textValue()
        return MessageVo(
            title = "$name 发动态了",
            body = body,
            toTopic = bellaTopic,
            fromClient = clientId,
            url = "https://t.bilibili.com/$dynamicId"
        )
    }

    private fun createVideoVo(it: JsonNode): MessageVo {
        val card: JsonNode = objectMapper.readValue(it["card"].textValue())
        val body = card["title"].textValue()
        val url = card["short_link"] ?: card["short_link_v2"]
        val name = it["desc"]["user_profile"]["info"]["uname"].textValue()
        return MessageVo(
            title = "$name 投稿了视频",
            body = body,
            toTopic = bellaTopic,
            fromClient = clientId,
            url = url.textValue()
        )
    }


    private fun sendMessage(message: MessageVo) {
        rabbitTemplate.convertAndSend(
            routingKey,
            objectMapper.writeValueAsString(message)
        )
    }
}
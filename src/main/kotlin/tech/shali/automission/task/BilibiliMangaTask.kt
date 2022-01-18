package tech.shali.automission.task

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import tech.shali.automission.dao.TaskParamDao
import kotlin.random.Random

/**
 *  b漫自动访问,用来获取每日阅读奖励
 */
@Component
class BilibiliMangaTask(
    private val taskParamDao: TaskParamDao,
    private val mailSender: JavaMailSender
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val noticeMail = (mailSender as JavaMailSenderImpl).username!!

    companion object {
        private const val ACCESS_KEY = "bilibili-manga-accessKey"
    }

    @Scheduled(cron = "0 0 3 * * *")
    fun run() {
        val randomSec = Random(System.currentTimeMillis()).nextLong(60)
        //睡眠1分钟内随机秒数
        Thread.sleep(randomSec * 1000)
        try {
            val accessKey = taskParamDao.findByKey(ACCESS_KEY).value
            val ts = System.currentTimeMillis() / 1000
            WebClient.create()
                .post()
                .uri(
                    "https://manga.bilibili.com/twirp/bookshelf.v1.Bookshelf/AddHistory?" +
                            "appkey=cc8617fd6961e070&mobi_app=android_comic&version=4.0.0&build=36400011&channel=pc_bilicomic&platform=android&device=android" +
                            "&buvid=XY1F7A61D38A07B5AFD5F11E4B1791A2BA973&machine=Xiaomi+MIX+2&access_key=$accessKey&is_teenager=0&ts=$ts"
                ).bodyValue("{\"comicId\":28562,\"epId\":494651}") //《安达与岛村》
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve().bodyToMono(MangaRes::class.java)
                .block()
                .also {
                    if (it?.code != "0")
                        throw RuntimeException(it?.msg)
                    logger.debug("code: ${it.code} msg: ${it.msg}")
                }
        } catch (e: Exception) {
            logger.warn(e.message)
            mailSender.send(SimpleMailMessage().apply {
                setFrom(noticeMail)
                setTo(noticeMail)
                setSubject("bilibili漫画自动任务失败")
                setText("错误信息: ${e.message}")
            })
        }
    }

    data class MangaRes(
        val code: String,
        val msg: String
    )
}
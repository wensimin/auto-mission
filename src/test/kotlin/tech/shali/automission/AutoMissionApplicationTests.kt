package tech.shali.automission

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.http.*
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.context.ActiveProfiles
import tech.shali.automission.entity.Task


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [TestConfig::class]
)
@ActiveProfiles("test")
class AutoMissionApplicationTests(
    @Autowired
    private val logger: Logger,
    @Autowired
    private val restTemplate: TestRestTemplate,
    @Autowired
    private val objectMapper: ObjectMapper
) {
    // 由于testRestTemplate 的mapper 有问题,手动替换到 注入的mapper
    init {
        restTemplate.restTemplate.messageConverters.first {
            it::class.simpleName == "MappingJackson2HttpMessageConverter"
        }.also { converter ->
            converter as MappingJackson2HttpMessageConverter
            converter.objectMapper = this.objectMapper
        }
    }

    companion object {
        val mockTask = Task(null, "test", "test", "val a=1", null, 999999)
    }

    @Test
    fun `should be unauthorized without bearer token`() {
        restTemplate.getForEntity<JsonNode>("/task")
            .also {
                logger.debug(it.body.toString())
                assert(it.statusCode == HttpStatus.UNAUTHORIZED)
            }
    }

    @Test
    fun `admin bearer token`() {
        restTemplate.exchange<RestPageImpl<Task>>(
            "/task", HttpMethod.GET, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        ).also {
            logger.debug(it.body.toString())
            assert(it.statusCode == HttpStatus.OK)
        }
    }

    @Test
    fun `no admin bearer token`() {
        restTemplate.exchange<JsonNode>(
            "/task", HttpMethod.GET, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer user")
            })
        ).also {
            logger.debug(it.body.toString())
            assert(it.statusCode == HttpStatus.FORBIDDEN)
        }
    }

    @Test
    fun `task curd work flow`() {
        `step 1 create task`()
        val id = `step 2 view list task`()
        `step 3 find one task`(id)
        `step 4 start task`(id)
        `step 5 stop task`(id)
        `step 6 delete task`(id)
    }


    private fun `step 1 create task`() {
        restTemplate.postForEntity<JsonNode>("/task", HttpEntity<Task>(mockTask, HttpHeaders().apply {
            set("Authorization", "Bearer admin")
            contentType = MediaType.APPLICATION_JSON
        })).also {
            logger.debug(it.body.toString())
            assert(it.statusCode == HttpStatus.OK)
        }
    }

    private fun `step 2 view list task`(): String {
        restTemplate.exchange<RestPageImpl<Task>>(
            "/task", HttpMethod.GET, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        ).also {
            assert(it.statusCode == HttpStatus.OK)
            assert(it.body!!.totalElements >= 1)
            it.body!!.content.first().run {
                assertTask(this, mockTask)
                return id.toString()
            }
        }
    }

    private fun `step 3 find one task`(id: String) {
        restTemplate.exchange<Task>(
            "/task/$id", HttpMethod.GET, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        ).also {
            assert(it.statusCode == HttpStatus.OK)
            assertTask(it.body!!, mockTask)
        }
    }

    private fun `step 4 start task`(id: String) {
        restTemplate.exchange<Task>(
            "/task/$id?enabled=true", HttpMethod.PUT, HttpEntity(
                null,
                HttpHeaders().apply {
                    set("Authorization", "Bearer admin")
                })
        )
        // check
        restTemplate.exchange<Task>(
            "/task/$id", HttpMethod.GET, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
                contentType = MediaType.APPLICATION_FORM_URLENCODED
            })
        ).also {
            assert(it.statusCode == HttpStatus.OK)
            it.body!!.also { task ->
                assert(task.enabled)
            }
        }
    }

    private fun `step 5 stop task`(id: String) {
        restTemplate.exchange<Task>(
            "/task/$id?enabled=false", HttpMethod.PUT, HttpEntity(
                null,
                HttpHeaders().apply {
                    set("Authorization", "Bearer admin")
                    contentType = MediaType.APPLICATION_FORM_URLENCODED
                })
        )
        //check
        restTemplate.exchange<Task>(
            "/task/$id", HttpMethod.GET, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        ).also {
            assert(it.statusCode == HttpStatus.OK)
            it.body!!.also { task ->
                assert(!task.enabled)
            }
        }
    }

    private fun `step 6 delete task`(id: String) {
        restTemplate.exchange<Task>(
            "/task/$id", HttpMethod.DELETE, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        ).also {
            assert(it.statusCode == HttpStatus.OK)
        }
        //check
        restTemplate.exchange<Void>(
            "/task/$id", HttpMethod.GET, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        ).also {
            assert(it.statusCode == HttpStatus.NOT_FOUND)
        }
    }


    private fun assertTask(task: Task, mockTask: Task) {
        assert(mockTask.name == task.name)
        assert(mockTask.description == task.description)
        assert(mockTask.code == task.code)
    }


    /**
     * 目前用于测试使用的page类
     */
    data class RestPageImpl<T>(val content: List<T>, val totalElements: Long)
}

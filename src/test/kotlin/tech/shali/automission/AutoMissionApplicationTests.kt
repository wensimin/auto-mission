package tech.shali.automission

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.http.*
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import tech.shali.automission.entity.Task
import tech.shali.automission.entity.TaskLog
import tech.shali.automission.pojo.*
import tech.shali.automission.service.TaskLogService
import tech.shali.automission.service.TaskLogger
import tech.shali.automission.service.TaskService
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*


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
    private val objectMapper: ObjectMapper,
    @Autowired
    private val taskService: TaskService
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
    fun `no auth`() {
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
        `create task`()
        val id = `view list task`()
        `find one task`(id)
        `start task`(id)
        `reload task`(id)
        `stop task`(id)
        `delete task`(id)
    }


    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun `dynamic query task`() {
        val mockTaskList = listOf(
            TaskSave(null, "apple", "这是个苹果", "val a=1", interval = 999999),
            TaskSave(null, "cat", "这是猫", "val a=1", interval = 999999),
            TaskSave(null, "appleB", "这是个苹果b", "val a=1", interval = 999999),
            TaskSave(null, "appleC", "这是个苹果c", "val a=1", interval = 999999),
            TaskSave(null, "appleD", "这是个苹果d", "val a=1", interval = 999999)
        )
        mockTaskList.forEach { task ->
            restTemplate.postForEntity<Void>("/task", HttpEntity(task, HttpHeaders().apply {
                set("Authorization", "Bearer admin")
                contentType = MediaType.APPLICATION_JSON
            })).also {
                logger.debug(it.body.toString())
                assert(it.statusCode == HttpStatus.OK)
            }
        }
        findTask(TaskQuery()).also {
            assert(it.totalElements.toInt() == mockTaskList.size)
        }
        findTask(TaskQuery(name = "cat")).also {
            assert(it.totalElements.toInt() == 1)
        }
        findTask(TaskQuery(name = "a")).also {
            assert(it.totalElements.toInt() == 5)
        }
        findTask(TaskQuery(description = "苹果")).also {
            assert(it.totalElements.toInt() == 4)
        }
        findTask(TaskQuery(description = "这是个苹果")).also {
            assert(it.totalElements.toInt() == 4)
        }
        findTask(TaskQuery(description = "猫")).also {
            assert(it.totalElements.toInt() == 1)
        }
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun `dynamic query log`(
        @Autowired
        taskLogService: TaskLogService,
        @Value("\${spring.webflux.format.date}")
        dateFormat: String
    ) {
        val taskLoggerA = taskLogService.getLogger("A-logger")
        val taskLoggerB = taskLogService.getLogger("B-logger")
        val format = SimpleDateFormat(dateFormat)
        val startDate = Date.from(Instant.now().minusSeconds(1))
        taskLoggerA.debug("a logger debug")
        taskLoggerA.warn("a logger warn")
        taskLoggerA.error("a logger error")
        taskLoggerA.info("info")
        Thread.sleep(5000)
        val dateCut = Date.from(Instant.now().minusSeconds(1))
        taskLoggerA.info("5s after")
        taskLoggerB.debug("debug")
        taskLoggerB.info("info")
        val endDate = Date.from(Instant.now().plusSeconds(1))
        findLog(TaskLogQuery(), format).also {
            //无条件查询会查询到系统非本case的log,所以使用>=
            assert(it.totalElements >= 6)
        }
        findLog(TaskLogQuery(taskId = "A-logger"), format).also {
            assert(it.totalElements.toInt() == 5)
        }
        findLog(TaskLogQuery(taskId = "A-logger", level = TaskLogger.Level.ERROR), format).also {
            assert(it.totalElements.toInt() == 1)
        }
        findLog(TaskLogQuery(taskId = "A-logger", level = TaskLogger.Level.INFO), format).also {
            assert(it.totalElements.toInt() == 4)
        }
        findLog(TaskLogQuery(taskId = "B-logger"), format).also {
            assert(it.totalElements.toInt() == 2)
        }
        findLog(TaskLogQuery(taskId = "A-logger", startDate = startDate), format).also {
            assert(it.totalElements.toInt() == 5)
        }
        findLog(TaskLogQuery(taskId = "A-logger", startDate = startDate, endDate = endDate), format).also {
            assert(it.totalElements.toInt() == 5)
        }
        findLog(TaskLogQuery(taskId = "A-logger", startDate = startDate, endDate = dateCut), format).also {
            assert(it.totalElements.toInt() == 4)
        }
        findLog(TaskLogQuery(taskId = "A-logger", startDate = dateCut, endDate = endDate), format).also {
            assert(it.totalElements.toInt() == 1)
        }
        findLog(TaskLogQuery(taskId = "B-logger", startDate = dateCut), format).also {
            assert(it.totalElements.toInt() == 2)
        }
        findLog(TaskLogQuery(taskId = "A-logger", text = "after"), format).also {
            assert(it.totalElements.toInt() == 1)
        }
    }

    @Test
    fun `debug code work flow`() {
        debugCode("val a=1")
        debugCode(
            "import tech.shali.automission.service.*\n" +
                    "val logger = bindings[\"logger\"] as TaskLogger\n" +
                        "logger.info(\"testInfo\")\n"
        ).also {
            assert(it.contains("testInfo"))
        }
    }

    private fun debugCode(code: String): String {
        // start debug
        var debugResult = restTemplate.postForEntity<DebugResult>(
            "/debug",
            HttpEntity<DebugCodeVo>(DebugCodeVo(code), HttpHeaders().apply {
                set("Authorization", "Bearer admin")
                contentType = MediaType.APPLICATION_JSON
            })
        ).run {
            assert(statusCode == HttpStatus.OK)
            body!!
        }
        //each to end
        while (!debugResult.end) {
            Thread.sleep(1000)
            restTemplate.exchange<DebugResult>(
                "/debug/${debugResult.id}",
                HttpMethod.GET,
                HttpEntity<Void>(HttpHeaders().apply {
                    set("Authorization", "Bearer admin")
                })
            ).run {
                assert(statusCode == HttpStatus.OK)
                debugResult = body ?: debugResult.apply {
                    end = true
                }
            }
        }
        return debugResult.consoleText
    }

    private fun findLog(query: TaskLogQuery, dateFormat: SimpleDateFormat): RestPageImpl<TaskLog> {
        val startDate = if (query.startDate != null) dateFormat.format(query.startDate) else ""
        val endDate = if (query.endDate != null) dateFormat.format(query.endDate) else ""
        val queryString = "level=${query.level ?: ""}&text=${query.text ?: ""}&taskId=${query.taskId ?: ""}" +
                "&startDate=${startDate}&endDate=${endDate}"
        return restTemplate.exchange<RestPageImpl<TaskLog>>(
            "/taskLog?$queryString",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        ).run {
            assert(statusCode == HttpStatus.OK)
            body!!
        }
    }

    private fun findTask(query: TaskQuery): RestPageImpl<Task> {
        val queryString =
            "name=${query.name ?: ""}&description=${query.description ?: ""}&enabled=${query.enabled ?: ""}"
        return restTemplate.exchange<RestPageImpl<Task>>(
            "/task?$queryString",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        ).run {
            assert(statusCode == HttpStatus.OK)
            body!!
        }
    }


    private fun `create task`() {
        restTemplate.postForEntity<JsonNode>("/task", HttpEntity<Task>(mockTask, HttpHeaders().apply {
            set("Authorization", "Bearer admin")
            contentType = MediaType.APPLICATION_JSON
        })).also {
            logger.debug(it.body.toString())
            assert(it.statusCode == HttpStatus.OK)
        }
    }

    private fun `view list task`(): String {
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

    private fun `find one task`(id: String) {
        restTemplate.exchange<Task>(
            "/task/$id", HttpMethod.GET, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        ).also {
            assert(it.statusCode == HttpStatus.OK)
            assertTask(it.body!!, mockTask)
        }
    }

    private fun `start task`(id: String) {
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
            })
        ).also {
            assert(it.statusCode == HttpStatus.OK)
            it.body!!.also { task ->
                assert(task.enabled)
            }
        }
    }

    private fun `reload task`(id: String) {
        runBlocking {
            taskService.reloadTask()
        }
        // check
        restTemplate.exchange<Task>(
            "/task/$id", HttpMethod.GET, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        ).also {
            assert(it.statusCode == HttpStatus.OK)
            it.body!!.also { task ->
                assert(task.enabled)
            }
        }
    }

    private fun `stop task`(id: String) {
        restTemplate.exchange<Task>(
            "/task/$id?enabled=false", HttpMethod.PUT, HttpEntity(
                null,
                HttpHeaders().apply {
                    set("Authorization", "Bearer admin")
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

    private fun `delete task`(id: String) {
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

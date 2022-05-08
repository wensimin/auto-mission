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
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Mono
import tech.shali.automission.entity.Store
import tech.shali.automission.entity.Task
import tech.shali.automission.entity.TaskLog
import tech.shali.automission.pojo.*
import tech.shali.automission.service.KVStore
import tech.shali.automission.service.TaskLogService
import tech.shali.automission.service.TaskLogger
import tech.shali.automission.service.TaskService
import tech.shali.automission.websocket.DebugHandler
import java.lang.Thread.sleep
import java.net.URI
import java.text.SimpleDateFormat
import java.time.Duration
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
        val mockTask = TaskSave(null, "test", "test", "val a=1", null, 999999)
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
    fun `cache work flow`(@Autowired kvStore: KVStore, @Autowired countSqlInterceptor: CountSqlInterceptor) {
        countSqlInterceptor.reset("store")
        val key = "key"
        // +1
        assert(kvStore.get(key) == null)
        // 0
        assert(kvStore.get(key) == null)
        assert(countSqlInterceptor.count() == 1)
        // +2
        kvStore.set(key, "aa")
        // 0
        assert(kvStore.get(key) == "aa")
        // 0
        assert(kvStore.get(key) == "aa")
        assert(countSqlInterceptor.count() == 3)
        // +2
        kvStore.del(key)
        // 0
        assert(kvStore.get(key) == null)
        assert(countSqlInterceptor.count() == 5)
    }

    @Test
    fun `task curd work flow`() {
        `create task`()
        val id = `view list task`()
        `find one task`(id)
        `start task`(id)
        `check instance size`(false, 1)
        `reload task`(id)
        `check instance size`(false, 1)
        `stop task`(id)
        `check instance size`(true, 2)
        `delete task`(id)
        `check instance size`(false, 0)
        `clear task instance`()
        `check instance size`(size = 0)
    }

    @Test
    @DirtiesContext
    fun `task instance curd flow`() {
        val task = saveTask(TaskSave(null, "测试instance", "这是个苹果", "val a=1", interval = 999999))
        `check instance size`(size = 0)
        `start task`(task.id.toString())
        val list = `get task instance`()
        assert(list.size == 1)
        `start task instance`(task.id.toString())
        `check instance size`(size = 2)
        sleep(5000)
        `clear task instance`()
        `check instance size`(size = 1)
        val longTask = saveTask(TaskSave(null, "长任务", "测试长任务", "while(true)Thread.sleep(100000)", interval = 999999))
        `start task instance`(longTask.id.toString())
        sleep(5000)
        val instances = `get task instance`(false).filter { it.get("name").textValue() == "长任务" }
        assert(instances.size == 1)
        `stop task instance`(instances.first().get("key").textValue())
        sleep(1000)
        assert(`get task instance`(false).none { it.get("name").textValue() == "长任务" })
    }


    @Test
    @DirtiesContext
    fun `dynamic query task`() {
        val mockTaskList = listOf(
            TaskSave(null, "apple", "这是个苹果", "val a=1", interval = 999999),
            TaskSave(null, "cat", "这是猫", "val a=1", interval = 999999),
            TaskSave(null, "appleB", "这是个苹果b", "val a=1", interval = 999999),
            TaskSave(null, "appleC", "这是个苹果c", "val a=1", interval = 999999),
            TaskSave(null, "appleD", "这是个苹果d", "val a=1", interval = 999999)
        )
        mockTaskList.forEach { task -> saveTask(task) }
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
    @DirtiesContext
    fun `dynamic query log`(
        @Autowired
        taskLogService: TaskLogService,
        @Value("\${spring.webflux.format.date}")
        dateFormat: String
    ) {
        val taskA = saveTask(TaskSave(null, "taskA", "taskA", "val a=1", interval = 999999))
        val taskB = saveTask(TaskSave(null, "taskB", "taskB", "val a=1", interval = 999999))
        val taskLoggerA = taskLogService.getLogger(taskA)
        val taskLoggerB = taskLogService.getLogger(taskB)
        val format = SimpleDateFormat(dateFormat)
        val startDate = Date.from(Instant.now().minusSeconds(1))
        taskLoggerA.debug("a logger debug")
        taskLoggerA.warn("a logger warn")
        taskLoggerA.error("a logger error")
        taskLoggerA.info("info")
        sleep(5000)
        val dateCut = Date.from(Instant.now().minusSeconds(1))
        taskLoggerA.info("5s after")
        taskLoggerB.debug("debug")
        taskLoggerB.info("info")
        val endDate = Date.from(Instant.now().plusSeconds(1))
        findLog(TaskLogQuery(), format).also {
            //无条件查询会查询到系统非本case的log,所以使用>=
            assert(it.totalElements >= 6)
        }
        findLog(TaskLogQuery(taskId = taskA.id, page = PageVo(size = 1)), format).also {
            assert(it.content.size == 1)
            assert(it.totalElements.toInt() == 5)
        }
        findLog(TaskLogQuery(taskId = taskA.id, level = TaskLogger.Level.ERROR), format).also {
            assert(it.totalElements.toInt() == 1)
        }
        findLog(TaskLogQuery(taskId = taskA.id, level = TaskLogger.Level.INFO), format).also {
            assert(it.totalElements.toInt() == 4)
        }
        findLog(TaskLogQuery(taskId = taskB.id), format).also {
            assert(it.totalElements.toInt() == 2)
        }
        findLog(TaskLogQuery(taskId = taskA.id, startDate = startDate), format).also {
            assert(it.totalElements.toInt() == 5)
        }
        findLog(TaskLogQuery(taskId = taskA.id, startDate = startDate, endDate = endDate), format).also {
            assert(it.totalElements.toInt() == 5)
        }
        findLog(TaskLogQuery(taskId = taskA.id, startDate = startDate, endDate = dateCut), format).also {
            assert(it.totalElements.toInt() == 4)
        }
        findLog(TaskLogQuery(taskId = taskA.id, startDate = dateCut, endDate = endDate), format).also {
            assert(it.totalElements.toInt() == 1)
        }
        findLog(TaskLogQuery(taskId = taskB.id, startDate = dateCut), format).also {
            assert(it.totalElements.toInt() == 2)
        }
        findLog(TaskLogQuery(taskId = taskA.id, text = "after"), format).also {
            assert(it.totalElements.toInt() == 1)
        }
        findLog(TaskLogQuery(taskName = "taskB"), format).also {
            assert(it.totalElements.toInt() == 2)
            it.content.forEach { log -> assert(log.task?.name == "taskB") }
        }
    }

    @Test
    @DirtiesContext
    fun `debug code work flow`() {
        debugCode("val a=1")
        debugCode(
            "import tech.shali.automission.service.*\n" +
                    "val logger = bindings[\"logger\"] as TaskLogger\n" +
                    "logger.info(\"testInfo\")\n"
        ).also {
            assert(it.contains("testInfo"))
        }
        `check instance size`(size = 2)
    }

    @Test
    fun `valid param`() {
        restTemplate.exchange<JsonNode>(
            "/store", HttpMethod.PUT, HttpEntity(
                Store("", "value"),
                HttpHeaders().apply {
                    set("Authorization", "Bearer admin")
                })
        ).also {
            assert(it.statusCode == HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @Test
    fun `use store work flow`() {
        saveStore(Store("key", "value")).also {
            assert(it.statusCode == HttpStatus.OK)
            assert(it.body?.key == "key")
            assert(it.body?.value == "value")
        }
        restTemplate.exchange<RestPageImpl<Store>>(
            "/store?key=ke", HttpMethod.GET, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        ).also {
            assert(it.statusCode == HttpStatus.OK)
            assert(it.body!!.totalElements >= 1)
        }
        val logStoreCode = "import tech.shali.automission.service.*\n" +
                "val store  =  bindings[\"store\"] as KVStore\n" +
                "val logger = bindings[\"logger\"] as TaskLogger\n" +
                "logger.info(store.get(\"key\")!!)"
        debugCode(logStoreCode).also {
            assert(it.contains("value"))
        }
        saveStore(Store("key", "newValue")).also {
            assert(it.statusCode == HttpStatus.OK)
            assert(it.body?.key == "key")
            assert(it.body?.value == "newValue")
        }
        debugCode(logStoreCode).also {
            assert(it.contains("newValue"))
        }
        restTemplate.exchange<Void>(
            "/store/key", HttpMethod.DELETE, HttpEntity(
                null,
                HttpHeaders().apply {
                    set("Authorization", "Bearer admin")
                })
        ).also {
            assert(it.statusCode == HttpStatus.OK)
        }
        restTemplate.exchange<RestPageImpl<Store>>(
            "/store?key=ke", HttpMethod.GET, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        ).also {
            assert(it.statusCode == HttpStatus.OK)
            assert(it.body!!.totalElements.toInt() == 0)
        }
    }

    @Test
    @DirtiesContext
    fun `websocket debug`(
        @LocalServerPort
        port: Int,
        @Value("\${spring.webflux.base-path}")
        basePath: String
    ) {
        val token = getWsToken()
        val code = "import tech.shali.automission.service.*\n" +
                "val logger = bindings[\"logger\"] as TaskLogger\n" +
                "logger.info(\"testInfo\")\n" +
                "logger.debug(\"testdebug\")\n"
        var count = 0
        ReactorNettyWebSocketClient().execute(
            URI.create("ws://localhost:${port}${basePath}debug-ws?$token")
        ) { session ->
            session.send(
                Mono.just(session.textMessage(code))
            )
                .thenMany(
                    session.receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .map { if (it.contains("test")) count++ }
                )
                .then()
        }.block(Duration.ofSeconds(20L))
        assert(count == 2)
    }


    @Test
    fun `no auth websocket debug`(
        @LocalServerPort
        port: Int,
        @Value("\${spring.webflux.base-path}")
        basePath: String
    ) {
        ReactorNettyWebSocketClient().execute(
            URI.create("ws://localhost:${port}${basePath}debug-ws")
        ) { session ->
            session.closeStatus().map {
                assert(it.code == DebugHandler.NO_AUTH.code)
            }.then()
        }.block(Duration.ofSeconds(20L))
    }

    @Test
    fun `websocket debug error`(
        @LocalServerPort
        port: Int,
        @Value("\${spring.webflux.base-path}")
        basePath: String
    ) {
        val token = getWsToken()
        ReactorNettyWebSocketClient().execute(
            URI.create("ws://localhost:${port}${basePath}debug-ws?$token")
        ) { session ->
            session.send(
                Mono.just(session.textMessage("aaaa"))
            ).and(
                session.receive()
                    .map { it.payloadAsText }
                    .map { assert(it.contains("Unresolved reference: aaaa")) }
            )
        }.block(Duration.ofSeconds(20L))
    }

    private fun getWsToken(): String {
        return restTemplate.exchange<String>(
            "/debug/ws/token", HttpMethod.GET, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        ).body!!
    }

    private fun saveStore(store: Store): ResponseEntity<Store> {
        return restTemplate.exchange(
            "/store", HttpMethod.PUT, HttpEntity(
                store,
                HttpHeaders().apply {
                    set("Authorization", "Bearer admin")
                })
        )
    }


    private fun saveTask(task: TaskSave): Task {
        return restTemplate.postForEntity<Task>("/task", HttpEntity(task, HttpHeaders().apply {
            set("Authorization", "Bearer admin")
            contentType = MediaType.APPLICATION_JSON
        })).let {
            logger.debug(it.body.toString())
            assert(it.statusCode == HttpStatus.OK)
            it.body!!
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
            sleep(1000)
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
        val pageQuery =
            "page.number=${query.page.number}&page.size=${query.page.size}"
        val queryString = "level=${query.level ?: ""}&text=${query.text ?: ""}" +
                "&taskId=${query.taskId ?: ""}&taskName=${query.taskName ?: ""}" +
                "&startDate=${startDate}&endDate=${endDate}&$pageQuery"
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
        saveTask(mockTask)
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

    private fun `get task instance`(done: Boolean? = null): List<JsonNode> {
        return restTemplate.exchange<List<JsonNode>>(
            "/task/instance?done=${done ?: ""}", HttpMethod.GET, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        ).body!!
    }

    private fun `start task instance`(id: String) {
        restTemplate.exchange<Void>(
            "/task/single/$id", HttpMethod.POST, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        ).also {
            assert(it.statusCode == HttpStatus.OK)
        }
    }

    private fun `stop task instance`(key: String) {
        restTemplate.exchange<Void>(
            "/task/instance/$key", HttpMethod.PUT, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        ).also {
            assert(it.statusCode == HttpStatus.OK)
        }
    }

    private fun `clear task instance`() {
        restTemplate.exchange<Void>(
            "/task/instance/", HttpMethod.DELETE, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        ).also {
            assert(it.statusCode == HttpStatus.OK)
        }
    }

    private fun `check instance size`(done: Boolean? = null, size: Int) {
        assert(`get task instance`(done).size == size)
    }


    private fun assertTask(task: Task, mockTask: TaskSave) {
        assert(mockTask.name == task.name)
        assert(mockTask.description == task.description)
        assert(mockTask.code == task.code)
    }


    /**
     * 目前用于测试使用的page类
     */
    data class RestPageImpl<T>(val content: List<T>, val totalElements: Long)
}

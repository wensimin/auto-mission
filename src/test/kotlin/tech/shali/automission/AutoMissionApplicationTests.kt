package tech.shali.automission

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [TestConfig::class]
)
@ActiveProfiles("test")
class AutoMissionApplicationTests(
    @Autowired
    private val logger: Logger
) {

    @Test
    fun contextLoads() {
    }

    @Test
    fun `should be unauthorized without bearer token`(@Autowired restTemplate: TestRestTemplate) {
        val response = restTemplate.getForEntity<JsonNode>("/auto-mission/task")
        logger.debug(response.body.toString())
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `admin bearer token`(@Autowired restTemplate: TestRestTemplate) {
        val response = restTemplate.exchange<JsonNode>(
            "/auto-mission/task", HttpMethod.GET, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer admin")
            })
        )
        logger.debug(response.body.toString())
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `no admin bearer token`(@Autowired restTemplate: TestRestTemplate) {
        val response = restTemplate.exchange<JsonNode>(
            "/auto-mission/task", HttpMethod.GET, HttpEntity<Void>(HttpHeaders().apply {
                set("Authorization", "Bearer user")
            })
        )
        logger.debug(response.body.toString())
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }
}

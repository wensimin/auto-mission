package tech.shali.automission

import org.hibernate.resource.jdbc.spi.StatementInspector
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import reactor.kotlin.core.publisher.toMono
import java.time.Instant

/**
 * 测试专用的一些配置
 */
@TestConfiguration
class TestConfig {
    /**
     * 自定义jwt decode
     * 只需要携带string admin or user 就可获得对应权限
     */
    @Bean
    fun jwtDecoder(): ReactiveJwtDecoder {
        return ReactiveJwtDecoder {
            val jwt = Jwt.withTokenValue("token").apply {
                header("alg", "none")
                issuedAt(Instant.now())
                expiresAt(Instant.now().plusSeconds(3600))
            }
            when (it) {
                "admin" -> jwt.claim("auth", listOf("ADMIN"))
                "user" -> {}
            }
            jwt.build().toMono()
        }
    }

    /**
     * 自定义sql count ,用于统计sql执行的条数
     */
    @Bean
    fun interceptorRegistration(countSqlInterceptor: StatementInspector): HibernatePropertiesCustomizer {
        return HibernatePropertiesCustomizer {
            it["hibernate.session_factory.statement_inspector"] = countSqlInterceptor
        }
    }


}
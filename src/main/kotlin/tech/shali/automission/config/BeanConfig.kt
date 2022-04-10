package tech.shali.automission.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InjectionPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter

@Configuration
class BeanConfig {

    /**
     * 注入logger
     */
    @Bean
    @Scope("prototype")
    fun logger(injectionPoint: InjectionPoint): Logger {
        return LoggerFactory.getLogger(
            injectionPoint.methodParameter?.containingClass // constructor
                ?: injectionPoint.field?.declaringClass // or field injection
        )
    }

    /**
     * 任务池配置
     */
    @Bean
    fun threadPoolTaskScheduler(): TaskScheduler {
        return ThreadPoolTaskScheduler().apply {
            isRemoveOnCancelPolicy = true
            poolSize=10
        }
    }

    /**
     * 自定义转换jwt的权限
     */
    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val jwtAuthenticationConverter = JwtAuthenticationConverter()
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter {
            // scope & auth 本项目 混用
            val scope = it.getClaim<Collection<String>?>("scope")
            val auth = it.getClaim<Collection<String>?>("auth")
            ((scope ?: emptyList()) + (auth ?: emptyList())).map { s -> SimpleGrantedAuthority(s) }
        }
        return jwtAuthenticationConverter
    }

}
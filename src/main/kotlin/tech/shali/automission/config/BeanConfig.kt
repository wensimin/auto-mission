package tech.shali.automission.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InjectionPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

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

}
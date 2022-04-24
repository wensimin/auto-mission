package tech.shali.automission

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling


@SpringBootApplication
@EnableScheduling
@EnableCaching
@ConfigurationPropertiesScan
class AutoMissionApplication

fun main(args: Array<String>) {
    runApplication<AutoMissionApplication>(*args)
}


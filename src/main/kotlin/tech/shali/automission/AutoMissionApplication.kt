package tech.shali.automission

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.core.io.ClassPathResource
import org.springframework.scheduling.annotation.EnableScheduling
import java.net.URLClassLoader

@SpringBootApplication
@EnableScheduling
class AutoMissionApplication

fun main(args: Array<String>) {
//    URLClassLoader(arrayOf(ClassPathResource("").url)).loadClass("AutoTaskuuid")
    runApplication<AutoMissionApplication>(*args)
}

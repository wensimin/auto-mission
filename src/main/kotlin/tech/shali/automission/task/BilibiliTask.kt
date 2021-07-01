package tech.shali.automission.task

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class BilibiliTask {

    @Scheduled(fixedRate = 1000)
    fun manga(){

        println("run")
    }
}
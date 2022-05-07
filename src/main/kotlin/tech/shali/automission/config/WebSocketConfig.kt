package tech.shali.automission.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import tech.shali.automission.websocket.DebugHandler

@Configuration
class WebSocketConfig {
    @Bean
    fun router(debugHandler: DebugHandler): SimpleUrlHandlerMapping {
        return SimpleUrlHandlerMapping().apply {
            order = 1
            urlMap = mapOf("debug-ws" to debugHandler)
        }
    }
}
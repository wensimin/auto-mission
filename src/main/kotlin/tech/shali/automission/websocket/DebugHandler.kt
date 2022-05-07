package tech.shali.automission.websocket

import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import tech.shali.automission.service.DebugWsService
import tech.shali.automission.service.WSDebugTaskLogger

@Service
class DebugHandler(private val debugWsService: DebugWsService) : WebSocketHandler {
    override fun handle(session: WebSocketSession): Mono<Void> {
        val logger = WSDebugTaskLogger()
        return session.send(logger.flux()
            .map { session.textMessage(it) }
            .doOnComplete { throw RuntimeException() }
        ).onErrorResume {
            debugWsService.stopTask(session.id)
            session.close()
        }
            .and(session.receive()
                .map { it.payloadAsText }
                .map { debugWsService.startTask(it, logger, session.id) }
                .doOnComplete { debugWsService.stopTask(session.id) }
            )
    }
}
package tech.shali.automission.service

import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink

class WSDebugTaskLogger : TaskLogger {
    private lateinit var sink: FluxSink<String>
    override fun log(level: TaskLogger.Level, message: String) {
        sink.next("$level : $message")
    }

    /**
     * 化为flux，应该最先执行这个方法
     */
    fun flux() = Flux.create<String> { sink = it }

    fun complete() = sink.complete()

    fun nextError(errorMessage: String) {
        sink.next(errorMessage)
        complete()
    }
}
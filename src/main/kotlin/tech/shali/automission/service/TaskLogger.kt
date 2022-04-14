package tech.shali.automission.service

interface TaskLogger {

    fun log(level: Level, message: String)

    fun debug(message: String, taskId: String? = null) = log(Level.DEBUG, message)
    fun info(message: String, taskId: String? = null) = log(Level.INFO, message)
    fun error(message: String, taskId: String? = null) = log(Level.ERROR, message)
    fun warn(message: String, taskId: String? = null) = log(Level.WARN, message)

    enum class Level {
        ERROR, WARN, INFO, DEBUG
    }
}


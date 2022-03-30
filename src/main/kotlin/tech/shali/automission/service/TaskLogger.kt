package tech.shali.automission.service

interface TaskLogger {

    fun log(level: Level, message: String, taskId: String? = null)

    fun debug(message: String, taskId: String? = null) = log(Level.DEBUG, message, taskId)
    fun info(message: String, taskId: String? = null) = log(Level.INFO, message, taskId)
    fun error(message: String, taskId: String? = null) = log(Level.ERROR, message, taskId)
    fun warn(message: String, taskId: String? = null) = log(Level.WARN, message, taskId)

    enum class Level {
        ERROR, WARN, INFO, DEBUG
    }
}


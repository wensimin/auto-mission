package tech.shali.automission.service

interface TaskLogger {

    fun log(label: Label, message: String, taskId: String? = null)

    fun debug(message: String, taskId: String? = null) = log(Label.DEBUG, message, taskId)
    fun info(message: String, taskId: String? = null) = log(Label.INFO, message, taskId)
    fun error(message: String, taskId: String? = null) = log(Label.ERROR, message, taskId)
    fun warn(message: String, taskId: String? = null) = log(Label.WARN, message, taskId)

    enum class Label {
        ERROR, WARN, INFO, DEBUG
    }
}


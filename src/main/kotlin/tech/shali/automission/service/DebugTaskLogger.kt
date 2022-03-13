package tech.shali.automission.service

/**
 * 用于debug时使用的logger
 */
class DebugTaskLogger : TaskLogger {

    private val consoleText = StringBuilder()

    override fun log(level: TaskLogger.Level, message: String, taskId: String?) {
        consoleText.append("$level : $message")
        consoleText.append("\n")
    }

    fun view(): String {
        return consoleText.toString()
    }
}
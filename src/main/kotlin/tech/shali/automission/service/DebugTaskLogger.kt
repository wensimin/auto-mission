package tech.shali.automission.service

/**
 * 用于debug时使用的logger
 */
class DebugTaskLogger : TaskLogger {

    private val consoleText = StringBuffer()

    override fun log(level: TaskLogger.Level, message: String) {
        consoleText.append("$level : $message")
        consoleText.append("\n")
    }

    fun view(): String {
        return consoleText.toString()
    }
}
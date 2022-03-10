package tech.shali.automission.service

/**
 * 用于debug时使用的logger
 */
class DebugTaskLogger : TaskLogger {

    private val consoleText: StringBuilder = StringBuilder()

    override fun log(label: TaskLogger.Label, message: String, taskId: String?) {
        consoleText.append("$label : $message")
        consoleText.append("\n")
    }

    fun view(): String {
        return consoleText.toString()
    }
}
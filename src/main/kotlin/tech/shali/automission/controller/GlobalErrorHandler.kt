package tech.shali.automission.controller

import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import tech.shali.automission.entity.TaskLog
import tech.shali.automission.service.TaskLogService


@ControllerAdvice
@ResponseBody
class GlobalErrorHandler(
    private val logger: Logger,
    private val taskLogService: TaskLogService
) {

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = [Exception::class])
    fun exception(e: Exception): ErrorResponse {
        //按未知错误处理
        logger.warn(cutStackTrace(e.stackTraceToString()))
        taskLogService.log(TaskLog("WARN", e.stackTraceToString().substring(0,250)))
        return ErrorResponse(ErrorType.ERROR, e.localizedMessage ?: "未知错误")
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = [MethodArgumentNotValidException::class])
    fun exception(e: MethodArgumentNotValidException): ErrorResponse {
        val fieldError = e.bindingResult.fieldError
        val message = fieldError!!.field + ":" + fieldError.defaultMessage
        return ErrorResponse(ErrorType.PARAM, message)
    }

    private fun cutStackTrace(stackTrace: String): String {
        //切2行log
        return stackTrace.split("\r\n\t").subList(0, 2).joinToString("\r\n\t")
    }
}

class ErrorResponse(val error: ErrorType, val message: String)
enum class ErrorType {
    ERROR, PARAM
}

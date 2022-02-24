package tech.shali.automission.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tech.shali.automission.dao.TaskLogDao
import tech.shali.automission.entity.TaskLog
import java.lang.RuntimeException

@RestController
@RequestMapping("test")
class TestController(val taskLogDao: TaskLogDao) {
    @GetMapping
    fun test(): String {
        throw RuntimeException("测试错误eeee")
        for (i in 1..1000) {
            taskLogDao.save(TaskLog("label", "testText"))
        }
        return "admin!"
    }
}
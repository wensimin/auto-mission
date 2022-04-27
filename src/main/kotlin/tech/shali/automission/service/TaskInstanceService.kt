package tech.shali.automission.service

import org.slf4j.Logger
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.config.Task
import org.springframework.stereotype.Service

@Service
class TaskInstanceService(
    // 任务调度器
    private val taskScheduler: TaskScheduler,
    private val logger: Logger
) {


    fun startTask(task:Task){

    }

}
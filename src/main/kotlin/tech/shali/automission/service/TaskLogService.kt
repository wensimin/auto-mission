package tech.shali.automission.service

import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import tech.shali.automission.dao.TaskLogDao
import tech.shali.automission.entity.TaskLog
import tech.shali.automission.pojo.TaskLogQuery
import javax.persistence.criteria.Predicate

@Service
class TaskLogService(private val taskLogDao: TaskLogDao) {

    fun find(taskLogQuery: TaskLogQuery): Page<TaskLog> {
        return taskLogDao.findAll(getQuery(taskLogQuery), taskLogQuery.page.toPageRequest())
    }

    private fun getQuery(taskLogQuery: TaskLogQuery): Specification<TaskLog> {
        return Specification { root, query, criteriaBuilder ->
            val specs = mutableListOf<Predicate>()
            criteriaBuilder.run {
                if (StringUtils.hasText(taskLogQuery.label))specs.add(equal(root.get<String>("label"), taskLogQuery.label))
                if (StringUtils.hasText(taskLogQuery.text))specs.add(like(root.get("text"), "%${taskLogQuery.text}%"))
                if (StringUtils.hasText(taskLogQuery.taskId))specs.add(equal(root.get<String>("taskId"), taskLogQuery.taskId))
                if (taskLogQuery.startDate != null)specs.add(greaterThanOrEqualTo(root.get("createDate"), taskLogQuery.startDate))
                if (taskLogQuery.endDate != null)specs.add(lessThanOrEqualTo(root.get("createDate"), taskLogQuery.endDate))
            }
            query.where(*specs.toTypedArray()).restriction
        }
    }

}
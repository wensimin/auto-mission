package tech.shali.automission

import org.hibernate.resource.jdbc.spi.StatementInspector
import org.springframework.stereotype.Service

@Service
class CountSqlInterceptor : StatementInspector {
    private var count = 0
    private var keyword: String? = null
    override fun inspect(sql: String): String {
        if (keyword != null && sql.contains(keyword!!)) {
            synchronized(this) {
                count++
            }
        }
        return sql
    }

    fun count() = count
    fun reset(keyword: String? = null) {
        this.keyword = keyword
        count = 0
    }
}
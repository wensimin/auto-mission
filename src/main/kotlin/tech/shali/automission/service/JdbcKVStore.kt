package tech.shali.automission.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import tech.shali.automission.dao.StoreDao
import tech.shali.automission.pojo.Store

@Service
class JdbcKVStore(private val storeDao: StoreDao) : KVStore {
    override fun get(key: String): String? {
        return storeDao.findByIdOrNull(key)?.value
    }

    override fun set(key: String, value: String?) {
        this.storeDao.save(Store(key, value))
    }
}
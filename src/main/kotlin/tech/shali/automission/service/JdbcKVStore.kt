package tech.shali.automission.service

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import tech.shali.automission.dao.StoreDao
import tech.shali.automission.entity.Store
import tech.shali.automission.pojo.StoreQuery

@Service
class JdbcKVStore(private val storeDao: StoreDao) : KVStore {

    @Cacheable("store")
    override fun get(key: String): String? {
        return storeDao.findByIdOrNull(key)?.value
    }

    @CachePut("store", key = "#key")
    override fun set(key: String, value: String?): String? {
        return this.storeDao.save(Store(key, value)).value
    }

    @CachePut("store", key = "#key")
    override fun del(key: String) {
        storeDao.deleteById(key)
    }

    @CacheEvict("store", key = "#store.key")
    fun save(store: Store): Store {
        return this.storeDao.save(store)
    }

    fun findPage(query: StoreQuery): Page<Store> {
        return storeDao.findAll(query.toSpecification<Store>(), query.page.toPageRequest())
    }
}
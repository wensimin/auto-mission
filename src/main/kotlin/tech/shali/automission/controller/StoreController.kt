package tech.shali.automission.controller

import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*
import tech.shali.automission.dao.StoreDao
import tech.shali.automission.entity.Store
import tech.shali.automission.pojo.StoreQuery
import javax.validation.Valid

@RestController
@RequestMapping("store")
class StoreController(private val storeDao: StoreDao) {

    @GetMapping
    fun get(query: StoreQuery): Page<Store> {
        return storeDao.findAll(query.toSpecification<Store>(), query.page.toPageRequest())
    }

    @PutMapping
    fun put(@RequestBody @Valid store: Store): Store {
        return storeDao.save(store)
    }

}
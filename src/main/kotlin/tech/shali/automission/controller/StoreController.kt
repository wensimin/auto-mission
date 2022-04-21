package tech.shali.automission.controller

import org.springframework.web.bind.annotation.*
import tech.shali.automission.dao.StoreDao
import tech.shali.automission.pojo.Store
import javax.validation.Valid

@RestController
@RequestMapping("store")
class StoreController(private val storeDao: StoreDao) {

    @GetMapping
    fun get(): List<Store> {
        return storeDao.findAll()
    }

    @PutMapping
    fun put(@RequestBody @Valid store: Store): Store {
        return storeDao.save(store)
    }

}
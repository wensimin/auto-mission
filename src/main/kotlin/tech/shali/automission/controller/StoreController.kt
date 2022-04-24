package tech.shali.automission.controller

import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*
import tech.shali.automission.entity.Store
import tech.shali.automission.pojo.StoreQuery
import tech.shali.automission.service.JdbcKVStore
import javax.validation.Valid

@RestController
@RequestMapping("store")
class StoreController(private val jdbcKVStore: JdbcKVStore) {

    @GetMapping
    fun get(query: StoreQuery): Page<Store> = jdbcKVStore.findPage(query)

    @PutMapping
    fun put(@RequestBody @Valid store: Store) = jdbcKVStore.save(store)

    @DeleteMapping("{key}")
    fun delete(@PathVariable key: String) = jdbcKVStore.del(key)

}
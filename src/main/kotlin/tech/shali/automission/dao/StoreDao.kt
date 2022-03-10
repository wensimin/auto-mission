package tech.shali.automission.dao

import org.springframework.data.jpa.repository.JpaRepository
import tech.shali.automission.pojo.Store

interface StoreDao : JpaRepository<Store, String> {
}
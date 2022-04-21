package tech.shali.automission.dao

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import tech.shali.automission.entity.Store

interface StoreDao : JpaRepository<Store, String>, JpaSpecificationExecutor<Store>
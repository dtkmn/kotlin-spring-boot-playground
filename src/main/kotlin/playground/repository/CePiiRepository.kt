package playground.repository

import playground.entity.CePiiEntity
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component

@Component
interface CePiiRepository : CrudRepository<CePiiEntity, String> {

    fun findFirstByExternalIdOrderByJoinedAtDesc(externalId: String?): CePiiEntity?

    @Modifying
    @Query("update CePiiEntity u set u.externalId = :externalId where u.customerId = :customerId")
    fun updateExternalId(@Param("customerId") customerId: String, @Param("externalId") externalId: String)
}

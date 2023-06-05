package playground.common.idempotencyjpa.jpa

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Lob
import jakarta.persistence.Table
import org.springframework.data.domain.Persistable
import playground.common.idempotencyjpa.jpa.IdempotencyPK
import java.time.Instant

@Entity
@Table(name = "commons_idempotency_record")
data class IdempotencyRecord(
    @EmbeddedId
    val key: IdempotencyPK,

    val createdAt: Instant,

    @Lob
    val data: String?,

    val data2: String?,

    val final: Boolean
) : Persistable<IdempotencyPK> {
    // Force always creating a new entity.
    override fun isNew() = true
    override fun getId() = key
}

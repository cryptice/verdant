package app.verdant.entity

import java.time.Instant
import java.util.UUID

data class ApiKey(
    val id: UUID,
    val userId: Long,
    val orgId: Long,
    val name: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val lastUsedAt: Instant? = null,
    val revokedAt: Instant? = null,
)

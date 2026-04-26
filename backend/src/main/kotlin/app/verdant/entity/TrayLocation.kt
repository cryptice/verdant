package app.verdant.entity

import java.time.Instant

data class TrayLocation(
    val id: Long? = null,
    val orgId: Long,
    val name: String,
    val createdAt: Instant = Instant.now(),
)

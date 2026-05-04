package app.verdant.entity

import java.time.Instant

data class Outlet(
    val id: Long? = null,
    val orgId: Long,
    val name: String,
    val channel: Channel,
    val contactInfo: String? = null,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

enum class Channel { FLORIST, FARMERS_MARKET, CSA, WEDDING, WHOLESALE, DIRECT, OTHER }

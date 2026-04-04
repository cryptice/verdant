package app.verdant.entity

import java.time.Instant

data class Customer(
    val id: Long? = null,
    val orgId: Long,
    val name: String,
    val channel: Channel,
    val contactInfo: String? = null,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
)

enum class Channel { FLORIST, FARMERS_MARKET, CSA, WEDDING, WHOLESALE, DIRECT, OTHER }

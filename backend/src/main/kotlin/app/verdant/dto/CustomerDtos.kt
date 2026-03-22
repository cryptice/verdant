package app.verdant.dto

import app.verdant.entity.Channel
import java.time.Instant

data class CustomerResponse(
    val id: Long,
    val name: String,
    val channel: Channel,
    val contactInfo: String?,
    val notes: String?,
    val createdAt: Instant,
)

data class CreateCustomerRequest(
    val name: String,
    val channel: Channel,
    val contactInfo: String? = null,
    val notes: String? = null,
)

data class UpdateCustomerRequest(
    val name: String? = null,
    val channel: Channel? = null,
    val contactInfo: String? = null,
    val notes: String? = null,
)

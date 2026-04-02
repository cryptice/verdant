package app.verdant.dto

import app.verdant.entity.Channel
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
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
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:NotNull
    val channel: Channel,
    @field:Size(max = 255)
    val contactInfo: String? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)

data class UpdateCustomerRequest(
    @field:Size(max = 255)
    val name: String? = null,
    val channel: Channel? = null,
    @field:Size(max = 255)
    val contactInfo: String? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)

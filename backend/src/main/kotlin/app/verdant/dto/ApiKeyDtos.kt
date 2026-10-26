package app.verdant.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateApiKeyRequest(
    @field:NotBlank @field:Size(max = 100)
    val name: String,
    @field:Min(1) @field:Max(365)
    val validDays: Int = 90,
)

data class ApiKeyResponse(
    val id: UUID,
    val name: String,
    val organizationId: Long,
    val createdAt: Instant,
    val expiresAt: Instant,
    val lastUsedAt: Instant?,
)

/** Returned once, when the key is created; [token] cannot be retrieved again. */
data class CreatedApiKeyResponse(val key: ApiKeyResponse, val token: String)

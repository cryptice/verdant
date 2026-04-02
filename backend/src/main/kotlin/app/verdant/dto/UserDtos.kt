package app.verdant.dto

import app.verdant.entity.Role
import jakarta.validation.constraints.Size
import java.time.Instant

data class UserResponse(
    val id: Long,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: Role,
    val language: String,
    val onboarding: String?,
    val createdAt: Instant
)

data class UpdateUserRequest(
    @field:Size(max = 255)
    val displayName: String? = null,
    @field:Size(max = 255)
    val avatarUrl: String? = null,
    @field:Size(max = 10)
    val language: String? = null,
)

data class UpdateOnboardingRequest(
    @field:Size(max = 100)
    val completedSteps: List<String>? = null,
    val dismissed: Boolean? = null,
)

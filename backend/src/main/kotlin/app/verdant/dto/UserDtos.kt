package app.verdant.dto

import app.verdant.entity.Role
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
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val language: String? = null,
)

data class UpdateOnboardingRequest(
    val completedSteps: List<String>? = null,
    val dismissed: Boolean? = null,
)

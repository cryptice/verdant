package app.verdant.entity

import java.time.Instant

data class User(
    val id: Long? = null,
    val googleSubject: String? = null,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val passwordHash: String? = null,
    val role: Role = Role.USER,
    val language: String = "sv",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

enum class Role { USER, ADMIN }

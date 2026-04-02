package app.verdant.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class GoogleAuthRequest(
    @field:NotBlank
    val idToken: String
)

data class AdminLoginRequest(
    @field:NotBlank @field:Email
    val email: String,
    @field:NotBlank
    val password: String
)

data class AuthResponse(val token: String, val user: UserResponse)

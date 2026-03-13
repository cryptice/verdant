package app.verdant.dto

data class GoogleAuthRequest(val idToken: String)
data class AuthResponse(val token: String, val user: UserResponse)

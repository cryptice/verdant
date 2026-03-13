package app.verdant.service

import app.verdant.auth.GoogleTokenVerifier
import app.verdant.auth.TokenService
import app.verdant.dto.AuthResponse
import app.verdant.dto.UserResponse
import app.verdant.entity.User
import app.verdant.repository.UserRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class AuthService(
    private val googleTokenVerifier: GoogleTokenVerifier,
    private val tokenService: TokenService,
    private val userRepository: UserRepository
) {
    @Transactional
    fun authenticateWithGoogle(idToken: String): AuthResponse {
        val claims = googleTokenVerifier.verify(idToken)

        val user = userRepository.findByGoogleSubject(claims.sub) ?: User().apply {
            googleSubject = claims.sub
            email = claims.email
            displayName = claims.name
            avatarUrl = claims.picture
        }.also { userRepository.persist(it) }

        // Update user info on each login
        user.email = claims.email
        user.displayName = claims.name
        user.avatarUrl = claims.picture

        val token = tokenService.generateToken(user)
        return AuthResponse(token, user.toResponse())
    }
}

fun User.toResponse() = UserResponse(
    id = id!!,
    email = email,
    displayName = displayName,
    avatarUrl = avatarUrl,
    role = role,
    createdAt = createdAt
)

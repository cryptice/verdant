package app.verdant.service

import app.verdant.auth.GoogleTokenVerifier
import app.verdant.auth.TokenService
import app.verdant.dto.AuthResponse
import app.verdant.dto.UserOrgMembership
import app.verdant.dto.UserResponse
import app.verdant.entity.Role
import app.verdant.entity.User
import app.verdant.repository.OrgMemberRepository
import app.verdant.repository.OrganizationRepository
import app.verdant.repository.UserRepository
import io.quarkus.elytron.security.common.BcryptUtil
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException

@ApplicationScoped
class AuthService(
    private val googleTokenVerifier: GoogleTokenVerifier,
    private val tokenService: TokenService,
    private val userRepository: UserRepository,
    private val orgMemberRepository: OrgMemberRepository,
    private val organizationRepository: OrganizationRepository,
) {
    fun authenticateWithGoogle(idToken: String): AuthResponse {
        val claims = googleTokenVerifier.verify(idToken)

        var user = userRepository.findByGoogleSubject(claims.sub)
        if (user == null) {
            user = userRepository.persist(
                User(
                    googleSubject = claims.sub,
                    email = claims.email,
                    displayName = claims.name,
                    avatarUrl = claims.picture,
                )
            )
        } else {
            user = user.copy(email = claims.email, displayName = claims.name, avatarUrl = claims.picture)
            userRepository.update(user)
        }

        val token = tokenService.generateToken(user)
        return AuthResponse(token, user.toResponse(fetchMemberships(user.id!!)))
    }

    fun authenticateAdmin(email: String, password: String): AuthResponse {
        val user = userRepository.findByEmail(email)
            ?: throw BadRequestException("Invalid email or password")

        if (user.role != Role.ADMIN)
            throw ForbiddenException("Admin access required")

        if (user.passwordHash == null || !BcryptUtil.matches(password, user.passwordHash))
            throw BadRequestException("Invalid email or password")

        val token = tokenService.generateToken(user)
        return AuthResponse(token, user.toResponse(fetchMemberships(user.id!!)))
    }

    fun fetchMemberships(userId: Long): List<UserOrgMembership> =
        orgMemberRepository.findByUserId(userId).mapNotNull { membership ->
            organizationRepository.findById(membership.orgId)?.let { org ->
                UserOrgMembership(
                    orgId = org.id!!,
                    orgName = org.name,
                    orgEmoji = org.emoji,
                    role = membership.role,
                )
            }
        }
}

fun User.toResponse(organizations: List<UserOrgMembership> = emptyList()) = UserResponse(
    id = id!!,
    email = email,
    displayName = displayName,
    avatarUrl = avatarUrl,
    role = role,
    language = language,
    onboarding = onboardingJson,
    advancedMode = advancedMode,
    organizations = organizations,
    createdAt = createdAt
)

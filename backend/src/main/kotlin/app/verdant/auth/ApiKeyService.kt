package app.verdant.auth

import app.verdant.dto.ApiKeyResponse
import app.verdant.dto.CreateApiKeyRequest
import app.verdant.dto.CreatedApiKeyResponse
import app.verdant.entity.ApiKey
import app.verdant.repository.ApiKeyRepository
import app.verdant.repository.OrgMemberRepository
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.NotFoundException
import org.eclipse.microprofile.jwt.JsonWebToken
import java.time.Duration
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class ApiKeyService(
    private val repo: ApiKeyRepository,
    private val members: OrgMemberRepository,
) {
    fun create(userId: Long, orgId: Long, request: CreateApiKeyRequest): CreatedApiKeyResponse {
        val key = repo.create(userId, orgId, request.name.trim(), Instant.now().plus(Duration.ofDays(request.validDays.toLong())))
        // Only the USER group: a key never carries the ADMIN role, whatever its owner's role.
        val token = Jwt.issuer("verdant-api")
            .subject(userId.toString())
            .groups(setOf("USER"))
            .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE)
            .claim(KEY_ID_CLAIM, key.id.toString())
            .claim(ORG_CLAIM, orgId)
            .expiresAt(key.expiresAt)
            .sign()
        return CreatedApiKeyResponse(key.toResponse(), token)
    }

    fun list(userId: Long, orgId: Long): List<ApiKeyResponse> = repo.findActive(userId, orgId).map { it.toResponse() }

    fun revoke(userId: Long, orgId: Long, id: UUID) {
        val key = repo.findById(id)?.takeIf { it.userId == userId && it.orgId == orgId } ?: throw NotFoundException("API key not found")
        repo.revoke(key.id)
    }

    /**
     * The key behind [jwt] when it is an API key token that is still valid: its row exists, matches the token's
     * user and organization, is neither revoked nor expired, and the user is still a member of the organization.
     */
    fun authorize(jwt: JsonWebToken): ApiKey? {
        if (!isApiKey(jwt)) return null
        val id = runCatching { UUID.fromString(jwt.getClaim<String>(KEY_ID_CLAIM)) }.getOrNull() ?: return null
        val key = repo.findById(id) ?: return null
        val orgId = jwt.getClaim<Any>(ORG_CLAIM)?.toString()?.toLongOrNull()
        val valid = key.userId.toString() == jwt.subject && key.orgId == orgId && key.revokedAt == null &&
            key.expiresAt.isAfter(Instant.now()) && members.isMember(key.orgId, key.userId)
        if (!valid) return null
        if (key.lastUsedAt == null || key.lastUsedAt.isBefore(Instant.now().minusSeconds(60))) repo.touch(key.id)
        return key
    }

    private fun ApiKey.toResponse() = ApiKeyResponse(id, name, orgId, createdAt, expiresAt, lastUsedAt)

    companion object {
        const val TOKEN_TYPE = "api_key"
        private const val TOKEN_TYPE_CLAIM = "token_type"
        private const val KEY_ID_CLAIM = "api_key"
        private const val ORG_CLAIM = "api_org"

        fun isApiKey(jwt: JsonWebToken) = jwt.getClaim<String>(TOKEN_TYPE_CLAIM) == TOKEN_TYPE
    }
}

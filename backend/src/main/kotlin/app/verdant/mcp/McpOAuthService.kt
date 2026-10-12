package app.verdant.mcp

import app.verdant.repository.OrgMemberRepository
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriBuilder
import org.eclipse.microprofile.jwt.JsonWebToken
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class McpOAuthService(
    private val repo: McpOAuthRepository,
    private val members: OrgMemberRepository,
    private val config: McpConfig,
) {
    fun register(name: String, redirects: List<String>): McpClient {
        if (name.isBlank() || name.length > 120 || redirects.isEmpty() || redirects.size > 10 || redirects.any { !validRedirect(it) })
            throw BadRequestException("Invalid client name or redirect URIs")
        return McpClient(UUID.randomUUID().toString(), name, redirects.distinct()).also(repo::createClient)
    }

    fun authorize(clientId: String?, redirect: String?, responseType: String?, scope: String?, resource: String?, state: String?, challenge: String?, method: String?): String {
        val client = clientId?.let(repo::client) ?: throw BadRequestException("Unknown client")
        if (redirect !in client.redirectUris) throw BadRequestException("Unregistered redirect URI")
        if (responseType != "code" || scope != McpConfig.READ_SCOPE || !config.acceptsResource(resource) || method != "S256" || challenge == null || !challenge.matches(Regex("[A-Za-z0-9_-]{43}")) || (state?.length ?: 0) > 2048)
            throw BadRequestException("Authorization requires read scope, resource and S256 PKCE")
        val request = McpAuthorization(UUID.randomUUID(), client.id, redirect!!, state, challenge, Instant.now().plusSeconds(600))
        repo.createRequest(request)
        return UriBuilder.fromUri("${config.frontend}/mcp/authorize").queryParam("request", request.id).build().toString()
    }

    fun details(id: UUID): Map<String, Any> {
        val request = repo.request(id) ?: throw NotFoundException("Authorization request expired")
        val client = repo.client(request.clientId) ?: throw NotFoundException()
        val uri = URI(request.redirectUri)
        return mapOf("clientName" to client.name, "callbackOrigin" to "${uri.scheme}://${uri.rawAuthority}", "scope" to McpConfig.READ_SCOPE)
    }

    @Transactional
    fun consent(id: UUID, userId: Long, orgId: Long?, approve: Boolean): String {
        if (approve && (orgId == null || !members.isMember(orgId, userId))) throw ForbiddenException("Organization membership required")
        val request = repo.request(id, consume = true) ?: throw NotFoundException("Authorization request expired or already used")
        val redirect = UriBuilder.fromUri(request.redirectUri).queryParam("iss", config.base)
        request.state?.let { redirect.queryParam("state", it) }
        if (!approve) return redirect.queryParam("error", "access_denied").build().toString()
        val grant = McpGrant(UUID.randomUUID(), request.clientId, userId, orgId!!, Instant.now().plus(Duration.ofDays(30)), null)
        repo.createGrant(grant)
        val code = McpSecrets.token()
        repo.createCode(McpSecrets.hash(code), grant, request)
        return redirect.queryParam("code", code).build().toString()
    }

    @Transactional
    fun token(type: String?, client: String?, code: String?, redirect: String?, verifier: String?, refresh: String?, resource: String?): Response {
        if (!config.acceptsResource(resource)) return error("invalid_target")
        if (client == null || repo.client(client) == null) return error("invalid_client")
        val grant = when (type) {
            "authorization_code" -> {
                if (code == null || redirect == null || verifier == null || !verifier.matches(Regex("[A-Za-z0-9._~-]{43,128}"))) return error("invalid_grant")
                val id = repo.consumeCode(McpSecrets.hash(code), client, redirect, McpSecrets.challenge(verifier)) ?: return error("invalid_grant")
                repo.grant(id) ?: return error("invalid_grant")
            }
            "refresh_token" -> {
                if (refresh == null) return error("invalid_grant")
                val hash = McpSecrets.hash(refresh)
                val stored = repo.refresh(hash) ?: return error("invalid_grant")
                if (stored.grant.clientId != client) return error("invalid_grant")
                if (stored.usedAt != null) {
                    // Return normally so transaction commits the replay-triggered revocation.
                    repo.revoke(stored.grant.id)
                    return error("invalid_grant")
                }
                if (!stored.expiresAt.isAfter(Instant.now())) return error("invalid_grant")
                repo.markRefreshUsed(hash)
                stored.grant
            }
            else -> return error("unsupported_grant_type")
        }
        if (!active(grant)) return error("invalid_grant")
        val nextRefresh = McpSecrets.token()
        repo.createRefresh(McpSecrets.hash(nextRefresh), grant)
        val access = Jwt.issuer("verdant-api").subject(grant.userId.toString()).audience(config.resource)
            .groups(setOf("MCP_READ")).claim("token_type", "mcp").claim("scope", McpConfig.READ_SCOPE)
            .claim("mcp_org", grant.orgId).claim("mcp_grant", grant.id.toString()).expiresIn(Duration.ofHours(1)).sign()
        return Response.ok(mapOf("access_token" to access, "token_type" to "Bearer", "expires_in" to 3600, "refresh_token" to nextRefresh, "scope" to McpConfig.READ_SCOPE))
            .header("Cache-Control", "no-store").header("Pragma", "no-cache").build()
    }

    fun delegatedGrant(jwt: JsonWebToken): McpGrant? {
        if (jwt.getClaim<String>("token_type") != "mcp" || config.resource !in jwt.audience.orEmpty() || jwt.getClaim<String>("scope") != McpConfig.READ_SCOPE) return null
        val id = runCatching { UUID.fromString(jwt.getClaim<String>("mcp_grant")) }.getOrNull() ?: return null
        val grant = repo.grant(id) ?: return null
        val org = jwt.getClaim<Any>("mcp_org")?.toString()?.toLongOrNull()
        return grant.takeIf { it.userId.toString() == jwt.subject && it.orgId == org && active(it) }
    }

    private fun active(g: McpGrant) = g.revokedAt == null && g.expiresAt.isAfter(Instant.now()) && members.isMember(g.orgId, g.userId)
    private fun error(code: String) = Response.status(400).entity(mapOf("error" to code)).header("Cache-Control", "no-store").build()

    companion object {
        fun validRedirect(value: String): Boolean = runCatching {
            val uri = URI(value)
            value.length <= 2048 && uri.isAbsolute && !uri.host.isNullOrBlank() && uri.rawFragment == null && uri.rawUserInfo == null &&
                (uri.scheme == "https" || uri.scheme == "http" && uri.host in setOf("localhost", "127.0.0.1", "[::1]"))
        }.getOrDefault(false)
    }
}

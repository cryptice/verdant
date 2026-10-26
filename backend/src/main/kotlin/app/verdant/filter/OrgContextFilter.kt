package app.verdant.filter

import app.verdant.auth.ApiKeyService
import app.verdant.repository.OrgMemberRepository
import app.verdant.mcp.McpOAuthService
import app.verdant.mcp.McpConfig
import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.jwt.JsonWebToken

@Provider
@Priority(Priorities.AUTHORIZATION + 1)
class OrgContextFilter(
    private val jwt: JsonWebToken,
    private val orgMemberRepository: OrgMemberRepository,
    private val orgContext: OrgContext,
    private val mcp: McpOAuthService,
    private val mcpConfig: McpConfig,
    private val apiKeys: ApiKeyService,
) : ContainerRequestFilter {

    // Paths that act on the account rather than one organization. New account-level paths belong here,
    // which also keeps them out of reach of API keys (see apiKeyBlockedPrefixes).
    private val exemptPrefixes = listOf(
        "api/auth/",
        "api/mcp/oauth/",
        "mcp/oauth/",
        ".well-known/",
        "api/users/me",
        "api/organizations",
        "api/invites",
        "api/admin/",
        "api/dev/",
        "api/openapi",
        "api/swagger-ui",
        "q/",
    )

    // API keys reach only their organization's data: no account, organization, admin or key management.
    private val apiKeyAllowedExempt = listOf("api/openapi", "api/swagger-ui")
    private val apiKeyBlockedPrefixes = exemptPrefixes.filterNot { it in apiKeyAllowedExempt } + "api/api-keys"

    override fun filter(requestContext: ContainerRequestContext) {
        val path = requestContext.uriInfo.path.removePrefix("/")

        if (path == "mcp") {
            val grant = mcp.delegatedGrant(jwt)
            if (grant == null) {
                requestContext.abortWith(Response.status(401).header("WWW-Authenticate", mcpConfig.challenge).build())
            } else {
                orgContext.orgId = grant.orgId
                orgContext.userId = grant.userId
            }
            return
        }
        // Delegated read-only credentials must never authorize the regular application API.
        if (jwt.getClaim<String>("token_type") == "mcp") {
            requestContext.abortWith(Response.status(403).build())
            return
        }

        if (ApiKeyService.isApiKey(jwt)) {
            val key = apiKeys.authorize(jwt)
            if (key == null) {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(mapOf("message" to "API key is invalid, expired or revoked")).build())
                return
            }
            if (apiKeyBlockedPrefixes.any { path.startsWith(it) }) {
                requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity(mapOf("message" to "API keys cannot use this endpoint")).build())
                return
            }
            val orgIdHeader = requestContext.getHeaderString("X-Organization-Id")
            if (!orgIdHeader.isNullOrBlank() && orgIdHeader.toLongOrNull() != key.orgId) {
                requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity(mapOf("message" to "API key belongs to another organization")).build())
                return
            }
            orgContext.orgId = key.orgId
            orgContext.userId = key.userId
            return
        }

        if (exemptPrefixes.any { path.startsWith(it) }) return

        val subject = jwt.subject ?: return

        val userId = subject.toLongOrNull() ?: return

        val orgIdHeader = requestContext.getHeaderString("X-Organization-Id")
        if (orgIdHeader.isNullOrBlank()) {
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(mapOf("message" to "Missing X-Organization-Id header"))
                    .build()
            )
            return
        }

        val orgId = orgIdHeader.toLongOrNull()
        if (orgId == null) {
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(mapOf("message" to "Invalid X-Organization-Id header"))
                    .build()
            )
            return
        }

        if (!orgMemberRepository.isMember(orgId, userId)) {
            requestContext.abortWith(
                Response.status(Response.Status.FORBIDDEN)
                    .entity(mapOf("message" to "Not a member of the specified organization"))
                    .build()
            )
            return
        }

        orgContext.orgId = orgId
        orgContext.userId = userId
    }
}

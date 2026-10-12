package app.verdant.mcp

import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType

@Path("/.well-known")
@Produces(MediaType.APPLICATION_JSON)
class McpMetadataResource(private val config: McpConfig) {
    @GET @Path("oauth-protected-resource")
    fun resourceMetadata() = mapOf("resource" to config.resource, "authorization_servers" to listOf(config.base), "scopes_supported" to listOf(McpConfig.READ_SCOPE), "bearer_methods_supported" to listOf("header"), "resource_name" to "Verdant")
    @GET @Path("oauth-protected-resource/mcp")
    fun mcpResourceMetadata() = resourceMetadata()
    @GET @Path("oauth-authorization-server")
    fun serverMetadata() = mapOf(
        "issuer" to config.base, "authorization_endpoint" to "${config.base}/mcp/oauth/authorize",
        "token_endpoint" to "${config.base}/mcp/oauth/token", "registration_endpoint" to "${config.base}/mcp/oauth/register",
        "response_types_supported" to listOf("code"), "grant_types_supported" to listOf("authorization_code", "refresh_token"),
        "token_endpoint_auth_methods_supported" to listOf("none"), "code_challenge_methods_supported" to listOf("S256"),
        "scopes_supported" to listOf(McpConfig.READ_SCOPE), "client_id_metadata_document_supported" to false,
        "authorization_response_iss_parameter_supported" to true,
    )
}

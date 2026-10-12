package app.verdant.mcp

import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.net.URI

data class RegisterMcpClient(val client_name: String = "MCP client", val redirect_uris: List<String> = emptyList(), val token_endpoint_auth_method: String = "none", val grant_types: List<String> = listOf("authorization_code", "refresh_token"), val response_types: List<String> = listOf("code"))
data class McpConsent(val orgId: Long? = null)

@Path("/mcp/oauth")
@Produces(MediaType.APPLICATION_JSON)
class McpOAuthResource(private val service: McpOAuthService) {
    @POST @Path("register") @Consumes(MediaType.APPLICATION_JSON)
    fun register(input: RegisterMcpClient): Response {
        if (input.token_endpoint_auth_method != "none" || input.response_types != listOf("code") || input.grant_types.any { it !in setOf("authorization_code", "refresh_token") }) throw BadRequestException("Only public authorization-code clients supported")
        val client = service.register(input.client_name, input.redirect_uris)
        return Response.status(201).entity(mapOf("client_id" to client.id, "client_name" to client.name, "redirect_uris" to client.redirectUris, "token_endpoint_auth_method" to "none", "grant_types" to listOf("authorization_code", "refresh_token"), "response_types" to listOf("code"))).header("Cache-Control", "no-store").build()
    }
    @GET @Path("authorize")
    fun authorize(@QueryParam("client_id") client: String?, @QueryParam("redirect_uri") redirect: String?, @QueryParam("response_type") response: String?, @QueryParam("scope") scope: String?, @QueryParam("resource") resource: String?, @QueryParam("state") state: String?, @QueryParam("code_challenge") challenge: String?, @QueryParam("code_challenge_method") method: String?) =
        Response.seeOther(URI(service.authorize(client, redirect, response, scope, resource, state, challenge, method))).header("Cache-Control", "no-store").build()
    @POST @Path("token") @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun token(@FormParam("grant_type") type: String?, @FormParam("client_id") client: String?, @FormParam("code") code: String?, @FormParam("redirect_uri") redirect: String?, @FormParam("code_verifier") verifier: String?, @FormParam("refresh_token") refresh: String?, @FormParam("resource") resource: String?) = service.token(type, client, code, redirect, verifier, refresh, resource)
}

package app.verdant.mcp

import io.quarkus.security.Authenticated
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.UUID

@Path("/api/mcp/oauth")
@Produces(MediaType.APPLICATION_JSON)
class McpConsentResource(private val service: McpOAuthService, private val repo: McpOAuthRepository, private val jwt: JsonWebToken) {
    @GET @Path("requests/{id}")
    fun request(@PathParam("id") id: UUID) = Response.ok(service.details(id)).header("Cache-Control", "no-store").build()
    @POST @Path("requests/{id}/approve") @Authenticated @Consumes(MediaType.APPLICATION_JSON)
    fun approve(@PathParam("id") id: UUID, consent: McpConsent) = Response.ok(mapOf("redirectUrl" to service.consent(id, userId(), consent.orgId, true))).header("Cache-Control", "no-store").build()
    @POST @Path("requests/{id}/deny") @Authenticated
    fun deny(@PathParam("id") id: UUID) = Response.ok(mapOf("redirectUrl" to service.consent(id, userId(), null, false))).header("Cache-Control", "no-store").build()
    @GET @Path("connections") @Authenticated
    fun connections() = Response.ok(repo.connections(userId())).header("Cache-Control", "no-store").build()
    @DELETE @Path("connections/{id}") @Authenticated
    fun revoke(@PathParam("id") id: UUID): Response {
        val g = repo.grant(id) ?: throw NotFoundException()
        if (g.userId != userId()) throw NotFoundException()
        repo.revoke(id)
        return Response.noContent().build()
    }
    private fun userId() = jwt.subject?.toLongOrNull() ?: throw NotAuthorizedException("Bearer")
}

package app.verdant.resource

import app.verdant.auth.ApiKeyService
import app.verdant.dto.ApiKeyResponse
import app.verdant.dto.CreateApiKeyRequest
import app.verdant.dto.CreatedApiKeyResponse
import app.verdant.filter.OrgContext
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/** Managing your own API keys for the current organization. API keys themselves are refused here (OrgContextFilter). */
@Path("/api/api-keys")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class ApiKeyResource(
    private val service: ApiKeyService,
    private val orgContext: OrgContext,
) {
    @GET
    fun list(): List<ApiKeyResponse> = service.list(orgContext.userId, orgContext.orgId)

    @POST
    fun create(@Valid request: CreateApiKeyRequest): Response {
        val created: CreatedApiKeyResponse = service.create(orgContext.userId, orgContext.orgId, request)
        return Response.status(Response.Status.CREATED).entity(created).header("Cache-Control", "no-store").build()
    }

    @DELETE
    @Path("/{id}")
    fun revoke(@PathParam("id") id: UUID): Response {
        service.revoke(orgContext.userId, orgContext.orgId, id)
        return Response.noContent().build()
    }
}

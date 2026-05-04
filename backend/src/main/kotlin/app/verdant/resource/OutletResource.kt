package app.verdant.resource

import app.verdant.dto.CreateOutletRequest
import app.verdant.dto.UpdateOutletRequest
import app.verdant.filter.OrgContext
import app.verdant.service.OutletService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/outlets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class OutletResource(
    private val service: OutletService,
    private val orgContext: OrgContext,
) {
    @GET
    fun list() = service.list(orgContext.orgId)

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.get(id, orgContext.orgId)

    @POST
    fun create(@Valid request: CreateOutletRequest): Response {
        val outlet = service.create(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(outlet).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateOutletRequest) =
        service.update(id, request, orgContext.orgId)

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.delete(id, orgContext.orgId)
        return Response.noContent().build()
    }
}

package app.verdant.resource

import app.verdant.dto.BulkLocationNoteRequest
import app.verdant.dto.CreateTrayLocationRequest
import app.verdant.dto.UpdateTrayLocationRequest
import app.verdant.filter.OrgContext
import app.verdant.service.TrayLocationService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/tray-locations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class TrayLocationResource(
    private val service: TrayLocationService,
    private val orgContext: OrgContext,
) {
    @GET
    fun list() = service.list(orgContext.orgId)

    @POST
    fun create(@Valid request: CreateTrayLocationRequest): Response =
        Response.status(Response.Status.CREATED)
            .entity(service.create(request, orgContext.orgId))
            .build()

    @PATCH
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateTrayLocationRequest) =
        service.update(id, request, orgContext.orgId)

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.delete(id, orgContext.orgId)
        return Response.noContent().build()
    }

    @POST
    @Path("/{id}/water")
    fun water(@PathParam("id") id: Long) = service.water(id, orgContext.orgId)

    @POST
    @Path("/{id}/note")
    fun note(@PathParam("id") id: Long, @Valid request: BulkLocationNoteRequest) =
        service.note(id, orgContext.orgId, request)
}

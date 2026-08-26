package app.verdant.resource

import app.verdant.dto.CreateGardenAreaEventRequest
import app.verdant.dto.CreateGardenAreaPhotoRequest
import app.verdant.dto.UpdateGardenAreaRequest
import app.verdant.filter.OrgContext
import app.verdant.service.GardenAreaService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class GardenAreaResource(
    private val areaService: GardenAreaService,
    private val orgContext: OrgContext,
) {
    @GET
    @Path("/areas/{id}")
    fun get(@PathParam("id") id: Long) = areaService.getArea(id, orgContext.orgId)

    @PUT
    @Path("/areas/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateGardenAreaRequest) =
        areaService.updateArea(id, request, orgContext.orgId)

    @DELETE
    @Path("/areas/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        areaService.deleteArea(id, orgContext.orgId)
        return Response.noContent().build()
    }

    @GET
    @Path("/areas/{id}/events")
    fun listEvents(@PathParam("id") id: Long, @QueryParam("limit") @DefaultValue("50") limit: Int) =
        areaService.listEvents(id, orgContext.orgId, limit)

    @POST
    @Path("/areas/{id}/events")
    fun logEvent(@PathParam("id") id: Long, @Valid request: CreateGardenAreaEventRequest): Response {
        val event = areaService.logEvent(id, request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(event).build()
    }

    @GET
    @Path("/areas/{id}/photos")
    fun listPhotos(@PathParam("id") id: Long) = areaService.listPhotos(id, orgContext.orgId)

    @POST
    @Path("/areas/{id}/photos")
    fun addPhoto(@PathParam("id") id: Long, @Valid request: CreateGardenAreaPhotoRequest) =
        areaService.addPhoto(id, request, orgContext.orgId)

    @DELETE
    @Path("/areas/{id}/photos/{photoId}")
    fun deletePhoto(@PathParam("id") id: Long, @PathParam("photoId") photoId: Long): Response {
        areaService.deletePhoto(id, photoId, orgContext.orgId)
        return Response.noContent().build()
    }
}

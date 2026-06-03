package app.verdant.resource

import app.verdant.dto.CreateBedPhotoRequest
import app.verdant.dto.CreateBedRequest
import app.verdant.dto.UpdateBedRequest
import app.verdant.filter.OrgContext
import app.verdant.service.BedService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class BedResource(
    private val bedService: BedService,
    private val orgContext: OrgContext
) {
    @GET
    @Path("/beds")
    fun listAll() = bedService.getAllBedsForUser(orgContext.orgId)

    @GET
    @Path("/beds/{id}")
    fun get(@PathParam("id") id: Long) = bedService.getBed(id, orgContext.orgId)

    @PUT
    @Path("/beds/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateBedRequest) =
        bedService.updateBed(id, request, orgContext.orgId)

    @DELETE
    @Path("/beds/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        bedService.deleteBed(id, orgContext.orgId)
        return Response.noContent().build()
    }

    @GET
    @Path("/beds/{id}/history")
    fun history(@PathParam("id") id: Long) = bedService.getBedHistory(id, orgContext.orgId)

    @GET
    @Path("/beds/{id}/harvest-stats")
    fun harvestStats(@PathParam("id") id: Long, @QueryParam("seasonId") seasonId: Long?) =
        bedService.getBedHarvestStats(id, orgContext.orgId, seasonId)

    @GET
    @Path("/beds/{id}/photos")
    fun listPhotos(@PathParam("id") id: Long) = bedService.listPhotos(id, orgContext.orgId)

    @POST
    @Path("/beds/{id}/photos")
    fun addPhoto(@PathParam("id") id: Long, @Valid request: CreateBedPhotoRequest) =
        bedService.addPhoto(id, request, orgContext.orgId)

    @DELETE
    @Path("/beds/{id}/photos/{photoId}")
    fun deletePhoto(@PathParam("id") id: Long, @PathParam("photoId") photoId: Long): Response {
        bedService.deletePhoto(id, photoId, orgContext.orgId)
        return Response.noContent().build()
    }
}

package app.verdant.resource

import app.verdant.dto.CreateSupplyApplicationRequest
import app.verdant.dto.SupplyApplicationResponse
import app.verdant.filter.OrgContext
import app.verdant.service.SupplyApplicationService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/supply-applications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class SupplyApplicationResource(
    private val service: SupplyApplicationService,
    private val orgContext: OrgContext,
) {
    @POST
    fun create(@Valid request: CreateSupplyApplicationRequest): Response {
        val created = service.create(request, orgContext.orgId, orgContext.userId)
        return Response.status(Response.Status.CREATED).entity(created).build()
    }

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long): SupplyApplicationResponse =
        service.findById(id, orgContext.orgId)

    @GET
    @Path("/bed/{bedId}")
    fun listByBed(
        @PathParam("bedId") bedId: Long,
        @QueryParam("limit") limit: Int?,
    ): List<SupplyApplicationResponse> =
        service.findByBed(bedId, orgContext.orgId, (limit ?: 20).coerceIn(1, 100))

    @GET
    @Path("/garden/{gardenId}")
    fun listByGarden(
        @PathParam("gardenId") gardenId: Long,
        @QueryParam("limit") limit: Int?,
    ): List<SupplyApplicationResponse> =
        service.findByGarden(gardenId, orgContext.orgId, (limit ?: 20).coerceIn(1, 100))

    @GET
    @Path("/tray-location/{trayLocationId}")
    fun listByTrayLocation(
        @PathParam("trayLocationId") trayLocationId: Long,
        @QueryParam("limit") limit: Int?,
    ): List<SupplyApplicationResponse> =
        service.findByTrayLocation(trayLocationId, orgContext.orgId, (limit ?: 20).coerceIn(1, 100))
}

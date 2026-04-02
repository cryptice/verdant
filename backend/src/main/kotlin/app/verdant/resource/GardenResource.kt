package app.verdant.resource

import app.verdant.dto.*
import app.verdant.service.BedService
import app.verdant.service.GardenService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.logging.Logger

@Path("/api/gardens")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class GardenResource(
    private val gardenService: GardenService,
    private val bedService: BedService,
    private val jwt: JsonWebToken
) {
    private val log = Logger.getLogger(GardenResource::class.java.name)
    private fun userId() = jwt.subject.toLong()

    @GET
    fun list() = gardenService.getGardensForUser(userId())

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long): GardenResponse {
        log.info("GET /api/gardens/$id for user ${userId()}")
        return gardenService.getGarden(id, userId())
    }

    @POST
    fun create(@Valid request: CreateGardenRequest): Response {
        val garden = gardenService.createGarden(request, userId())
        return Response.status(Response.Status.CREATED).entity(garden).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateGardenRequest) =
        gardenService.updateGarden(id, request, userId())

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        gardenService.deleteGarden(id, userId())
        return Response.noContent().build()
    }

    @GET
    @Path("/{gardenId}/beds")
    fun listBeds(@PathParam("gardenId") gardenId: Long) =
        bedService.getBedsForGarden(gardenId, userId())

    @POST
    @Path("/{gardenId}/beds")
    fun createBed(@PathParam("gardenId") gardenId: Long, @Valid request: CreateBedRequest): Response {
        val bed = bedService.createBed(gardenId, request, userId())
        return Response.status(Response.Status.CREATED).entity(bed).build()
    }

    @POST
    @Path("/suggest-layout")
    fun suggestLayout(@Valid request: SuggestLayoutRequest) =
        gardenService.suggestLayout(request)

    @POST
    @Path("/with-layout")
    fun createWithLayout(@Valid request: CreateGardenWithLayoutRequest): Response {
        log.info("POST /api/gardens/with-layout: name=${request.name}, beds=${request.beds.size}")
        val result = gardenService.createGardenWithLayout(request, userId())
        log.info("Created garden id=${result.garden.id}")
        return Response.status(Response.Status.CREATED).entity(result).build()
    }
}

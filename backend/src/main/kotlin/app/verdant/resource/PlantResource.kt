package app.verdant.resource

import app.verdant.dto.*
import app.verdant.repository.UserRepository
import app.verdant.service.AiService
import app.verdant.service.PlantService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class PlantResource(
    private val plantService: PlantService,
    private val aiService: AiService,
    private val userRepository: UserRepository,
    private val jwt: JsonWebToken
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    @Path("/plants")
    fun listAll(
        @QueryParam("status") status: String?,
        @QueryParam("seasonId") seasonId: Long?,
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = plantService.getAllPlantsForUser(userId(), status?.let { app.verdant.entity.PlantStatus.valueOf(it) }, seasonId, limit.coerceIn(1, 200), offset.coerceAtLeast(0))

    @GET
    @Path("/beds/{bedId}/plants")
    fun list(@PathParam("bedId") bedId: Long, @QueryParam("seasonId") seasonId: Long?) =
        plantService.getPlantsForBed(bedId, userId(), seasonId)

    @GET
    @Path("/plants/{id}")
    fun get(@PathParam("id") id: Long) = plantService.getPlant(id, userId())

    @POST
    @Path("/beds/{bedId}/plants")
    fun create(@PathParam("bedId") bedId: Long, @Valid request: CreatePlantRequest): Response {
        val plant = plantService.createPlant(bedId, request, userId())
        return Response.status(Response.Status.CREATED).entity(plant).build()
    }

    @POST
    @Path("/plants")
    fun createWithoutBed(@Valid request: CreatePlantRequest): Response {
        val plant = plantService.createPlant(null, request, userId())
        return Response.status(Response.Status.CREATED).entity(plant).build()
    }

    @POST
    @Path("/plants/batch-sow")
    fun batchSow(@Valid request: BatchSowRequest): Response {
        val result = plantService.batchSow(request, userId())
        return Response.status(Response.Status.CREATED).entity(result).build()
    }

    @GET
    @Path("/plants/groups")
    fun plantGroups(@QueryParam("status") status: String, @QueryParam("trayOnly") trayOnly: Boolean?) =
        plantService.getPlantGroups(userId(), status, trayOnly == true)

    @GET
    @Path("/plants/tray-summary")
    fun traySummary() = plantService.getTraySummary(userId())

    @POST
    @Path("/plants/batch-event")
    fun batchEvent(@Valid request: BatchEventRequest): Response {
        val result = plantService.batchEvent(request, userId())
        return Response.ok(result).build()
    }

    @PUT
    @Path("/plants/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdatePlantRequest) =
        plantService.updatePlant(id, request, userId())

    @DELETE
    @Path("/plants/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        plantService.deletePlant(id, userId())
        return Response.noContent().build()
    }

    // ── Species Plant Summary ──

    @GET
    @Path("/plants/species-summary")
    fun speciesSummary() = plantService.getSpeciesPlantSummary(userId())

    @GET
    @Path("/plants/species/{speciesId}/locations")
    fun speciesLocations(@PathParam("speciesId") speciesId: Long) =
        plantService.getSpeciesLocations(userId(), speciesId)

    // ── Plant Events ──

    @GET
    @Path("/plants/{id}/events")
    fun listEvents(@PathParam("id") id: Long) = plantService.getEventsForPlant(id, userId())

    @POST
    @Path("/plants/{id}/events")
    fun addEvent(@PathParam("id") id: Long, @Valid request: CreatePlantEventRequest): Response {
        val event = plantService.addEvent(id, request, userId())
        return Response.status(Response.Status.CREATED).entity(event).build()
    }

    @DELETE
    @Path("/plants/{id}/events/{eventId}")
    fun deleteEvent(@PathParam("id") id: Long, @PathParam("eventId") eventId: Long): Response {
        plantService.deleteEvent(id, eventId, userId())
        return Response.noContent().build()
    }

    // ── Plant Identification ──

    @POST
    @Path("/plants/identify")
    fun identify(@Valid request: IdentifyPlantRequest): List<PlantSuggestion> {
        val language = userRepository.findById(userId())?.language ?: "sv"
        return aiService.identifyPlant(request.imageBase64, language)
    }
}

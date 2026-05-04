package app.verdant.resource

import app.verdant.dto.*
import app.verdant.filter.OrgContext
import app.verdant.repository.UserRepository
import app.verdant.repository.SaleLotRepository
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
    private val orgContext: OrgContext,
    private val jwt: JsonWebToken,
    private val saleLotRepository: SaleLotRepository,
) {

    @GET
    @Path("/plants/{id}/available-for-sale")
    fun availableForPlant(@PathParam("id") id: Long): Map<String, Int> {
        // Org check via the existing service path; the repo answer is the same regardless.
        plantService.getPlant(id, orgContext.orgId)
        return mapOf("available" to saleLotRepository.availableForPlant(id))
    }

    @GET
    @Path("/harvest-events/{id}/available-for-sale")
    fun availableForHarvestEvent(@PathParam("id") id: Long): Map<String, Int> {
        plantService.findEventInOrg(id, orgContext.orgId)
            ?: throw NotFoundException("Harvest event not found")
        return mapOf("available" to saleLotRepository.availableForHarvestEvent(id))
    }

    @GET
    @Path("/plants")
    fun listAll(
        @QueryParam("status") status: String?,
        @QueryParam("seasonId") seasonId: Long?,
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = plantService.getAllPlantsForUser(orgContext.orgId, status?.let { app.verdant.entity.PlantStatus.valueOf(it) }, seasonId, limit.coerceIn(1, 200), offset.coerceAtLeast(0))

    @GET
    @Path("/beds/{bedId}/plants")
    fun list(@PathParam("bedId") bedId: Long, @QueryParam("seasonId") seasonId: Long?) =
        plantService.getPlantsForBed(bedId, orgContext.orgId, seasonId)

    @GET
    @Path("/plants/{id}")
    fun get(@PathParam("id") id: Long) = plantService.getPlant(id, orgContext.orgId)

    @POST
    @Path("/beds/{bedId}/plants")
    fun create(@PathParam("bedId") bedId: Long, @Valid request: CreatePlantRequest): Response {
        val plant = plantService.createPlant(bedId, request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(plant).build()
    }

    @POST
    @Path("/plants")
    fun createWithoutBed(@Valid request: CreatePlantRequest): Response {
        val plant = plantService.createPlant(null, request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(plant).build()
    }

    @POST
    @Path("/plants/batch-sow")
    fun batchSow(@Valid request: BatchSowRequest): Response {
        val result = plantService.batchSow(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(result).build()
    }

    @GET
    @Path("/plants/groups")
    fun plantGroups(@QueryParam("status") status: String, @QueryParam("trayOnly") trayOnly: Boolean?) =
        plantService.getPlantGroups(orgContext.orgId, status, trayOnly == true)

    @GET
    @Path("/plants/tray-summary")
    fun traySummary() = plantService.getTraySummary(orgContext.orgId)

    @POST
    @Path("/plants/batch-event")
    fun batchEvent(@Valid request: BatchEventRequest): Response {
        val result = plantService.batchEvent(request, orgContext.orgId)
        return Response.ok(result).build()
    }

    @POST
    @Path("/plants/move-tray")
    fun moveTrayPlants(@Valid request: MoveTrayPlantsRequest) =
        plantService.moveTrayPlants(orgContext.orgId, request)

    @POST
    @Path("/beds/{id}/weed")
    fun weedBed(@PathParam("id") id: Long) = plantService.weedBed(id, orgContext.orgId)

    @POST
    @Path("/beds/{id}/water")
    fun waterBed(@PathParam("id") id: Long) = plantService.waterBed(id, orgContext.orgId)

    @GET
    @Path("/beds/{id}/events")
    fun listBedEvents(
        @PathParam("id") id: Long,
        @QueryParam("limit") @DefaultValue("50") limit: Int,
    ) = plantService.listBedEvents(id, orgContext.orgId, limit.coerceIn(1, 200))

    @PUT
    @Path("/plants/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdatePlantRequest) =
        plantService.updatePlant(id, request, orgContext.orgId)

    @DELETE
    @Path("/plants/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        plantService.deletePlant(id, orgContext.orgId)
        return Response.noContent().build()
    }

    // ── Species Plant Summary ──

    @GET
    @Path("/plants/species-summary")
    fun speciesSummary() = plantService.getSpeciesPlantSummary(orgContext.orgId)

    @GET
    @Path("/plants/species/{speciesId}/locations")
    fun speciesLocations(@PathParam("speciesId") speciesId: Long) =
        plantService.getSpeciesLocations(orgContext.orgId, speciesId)

    @GET
    @Path("/plants/species/{speciesId}/events")
    fun speciesEvents(
        @PathParam("speciesId") speciesId: Long,
        @QueryParam("trayOnly") @DefaultValue("false") trayOnly: Boolean,
    ) = plantService.getSpeciesEventSummary(orgContext.orgId, speciesId, trayOnly)

    @PATCH
    @Path("/plants/species/{speciesId}/events/date")
    fun updateSpeciesEventDate(
        @PathParam("speciesId") speciesId: Long,
        request: UpdateSpeciesEventDateRequest,
    ) = plantService.updateSpeciesEventDate(orgContext.orgId, speciesId, request)

    @POST
    @Path("/plants/species/{speciesId}/events/delete")
    fun deleteSpeciesEvent(
        @PathParam("speciesId") speciesId: Long,
        request: DeleteSpeciesEventRequest,
    ) = plantService.deleteSpeciesEvent(orgContext.orgId, speciesId, request)

    // ── Plant Events ──

    @GET
    @Path("/plants/{id}/events")
    fun listEvents(@PathParam("id") id: Long) = plantService.getEventsForPlant(id, orgContext.orgId)

    @POST
    @Path("/plants/{id}/events")
    fun addEvent(@PathParam("id") id: Long, @Valid request: CreatePlantEventRequest): Response {
        val event = plantService.addEvent(id, request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(event).build()
    }

    @DELETE
    @Path("/plants/{id}/events/{eventId}")
    fun deleteEvent(@PathParam("id") id: Long, @PathParam("eventId") eventId: Long): Response {
        plantService.deleteEvent(id, eventId, orgContext.orgId)
        return Response.noContent().build()
    }

    // ── Plant Identification ──

    @POST
    @Path("/plants/identify")
    fun identify(@Valid request: IdentifyPlantRequest): List<PlantSuggestion> {
        val language = userRepository.findById(jwt.subject.toLong())?.language ?: "sv"
        return aiService.identifyPlant(request.imageBase64, language)
    }
}

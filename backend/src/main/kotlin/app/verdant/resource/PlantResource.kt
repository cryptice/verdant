package app.verdant.resource

import app.verdant.dto.CreatePlantEventRequest
import app.verdant.dto.CreatePlantRequest
import app.verdant.dto.IdentifyPlantRequest
import app.verdant.dto.PlantSuggestion
import app.verdant.dto.UpdatePlantRequest
import app.verdant.repository.UserRepository
import app.verdant.service.AiService
import app.verdant.service.PlantService
import io.quarkus.security.Authenticated
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
    fun listAll(@QueryParam("status") status: String?) =
        plantService.getAllPlantsForUser(userId(), status?.let { app.verdant.entity.PlantStatus.valueOf(it) })

    @GET
    @Path("/beds/{bedId}/plants")
    fun list(@PathParam("bedId") bedId: Long) = plantService.getPlantsForBed(bedId, userId())

    @GET
    @Path("/plants/{id}")
    fun get(@PathParam("id") id: Long) = plantService.getPlant(id, userId())

    @POST
    @Path("/beds/{bedId}/plants")
    fun create(@PathParam("bedId") bedId: Long, request: CreatePlantRequest): Response {
        val plant = plantService.createPlant(bedId, request, userId())
        return Response.status(Response.Status.CREATED).entity(plant).build()
    }

    @PUT
    @Path("/plants/{id}")
    fun update(@PathParam("id") id: Long, request: UpdatePlantRequest) =
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
    fun addEvent(@PathParam("id") id: Long, request: CreatePlantEventRequest): Response {
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
    fun identify(request: IdentifyPlantRequest): List<PlantSuggestion> {
        val language = userRepository.findById(userId())?.language ?: "sv"
        return aiService.identifyPlant(request.imageBase64, language)
    }
}

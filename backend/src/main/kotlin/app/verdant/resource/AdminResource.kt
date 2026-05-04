package app.verdant.resource

import app.verdant.dto.*
import app.verdant.entity.Outlet
import app.verdant.entity.Provider
import app.verdant.entity.Role
import app.verdant.repository.GardenRepository
import app.verdant.repository.OrganizationRepository
import app.verdant.repository.OutletRepository
import app.verdant.repository.ProviderRepository
import app.verdant.repository.UserRepository
import app.verdant.repository.WorkflowRepository
import app.verdant.service.AiService
import app.verdant.service.SpeciesService
import app.verdant.service.toResponse
import jakarta.annotation.security.RolesAllowed
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/admin")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
class AdminResource(
    private val userRepository: UserRepository,
    private val gardenRepository: GardenRepository,
    private val speciesService: SpeciesService,
    private val aiService: AiService,
    private val providerRepository: ProviderRepository,
    private val workflowRepository: WorkflowRepository,
    private val outletRepository: OutletRepository,
    private val organizationRepository: OrganizationRepository,
) {
    @GET
    @Path("/users")
    fun listUsers() = userRepository.listAll().map { it.toResponse() }

    @DELETE
    @Path("/users/{id}")
    fun deleteUser(@PathParam("id") id: Long): Response {
        val user = userRepository.findById(id) ?: throw NotFoundException("User not found")
        if (user.role == Role.ADMIN) throw ForbiddenException("Admin users cannot be deleted")
        userRepository.delete(id)
        return Response.noContent().build()
    }

    @GET
    @Path("/gardens")
    fun listGardens() = gardenRepository.listAll().map { it.toResponse() }

    // ── Species ──

    @GET
    @Path("/species")
    fun listSpecies(@QueryParam("q") query: String?, @QueryParam("limit") limit: Int?) =
        if (query.isNullOrBlank()) speciesService.getAllSpecies()
        else speciesService.searchAllSpecies(query.trim(), limit ?: 20)

    @GET
    @Path("/species/export")
    fun exportSpecies() = speciesService.exportSpecies()

    @POST
    @Path("/species/import")
    @Consumes(MediaType.APPLICATION_JSON)
    fun importSpecies(entries: List<SpeciesExportEntry>) = speciesService.importSpecies(entries)

    @POST
    @Path("/species/extract-front")
    @Consumes(MediaType.APPLICATION_JSON)
    fun extractFrontInfo(@Valid request: ExtractSpeciesInfoRequest): Response {
        val info = aiService.extractFrontInfo(request.imageBase64)
            ?: return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "Could not extract names from front image"))
                .build()
        return Response.ok(info).build()
    }

    @POST
    @Path("/species/extract-back")
    @Consumes(MediaType.APPLICATION_JSON)
    fun extractBackInfo(@Valid request: ExtractSpeciesInfoRequest): Response {
        val info = aiService.extractSpeciesInfo(request.imageBase64, "sv")
            ?: return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "Could not extract species info from image"))
                .build()
        return Response.ok(info).build()
    }

    @GET
    @Path("/species/{id}")
    fun getSpecies(@PathParam("id") id: Long) = speciesService.getSpeciesAdmin(id)

    @POST
    @Path("/species")
    @Consumes(MediaType.APPLICATION_JSON)
    fun createSpecies(@Valid request: CreateSpeciesRequest) = speciesService.createSpeciesAdmin(request)

    @PUT
    @Path("/species/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    fun updateSpecies(@PathParam("id") id: Long, @Valid request: UpdateSpeciesRequest) =
        speciesService.updateSpeciesAdmin(id, request)

    @DELETE
    @Path("/species/{id}")
    fun deleteSpecies(@PathParam("id") id: Long): Response {
        speciesService.deleteSpeciesAdmin(id)
        return Response.noContent().build()
    }

    // ── Species Photos ──

    @POST
    @Path("/species/{id}/photos")
    @Consumes(MediaType.APPLICATION_JSON)
    fun uploadSpeciesPhoto(@PathParam("id") id: Long, @Valid request: UploadPhotoRequest) =
        speciesService.uploadSpeciesPhoto(id, request.imageBase64)

    @DELETE
    @Path("/species/{id}/photos/{photoId}")
    fun deleteSpeciesPhoto(@PathParam("id") id: Long, @PathParam("photoId") photoId: Long): Response {
        speciesService.deleteSpeciesPhoto(id, photoId)
        return Response.noContent().build()
    }

    // ── Tags ──

    @GET
    @Path("/species/tags")
    fun listTags() = speciesService.getAllTags()

    // ── Providers ──

    @GET
    @Path("/providers")
    fun listProviders() = providerRepository.findAll().map { ProviderResponse(it.id!!, it.name, it.identifier) }

    @POST
    @Path("/providers")
    @Consumes(MediaType.APPLICATION_JSON)
    fun createProvider(@Valid request: CreateProviderRequest): Response {
        val provider = providerRepository.persist(Provider(name = request.name, identifier = request.identifier))
        return Response.status(Response.Status.CREATED).entity(ProviderResponse(provider.id!!, provider.name, provider.identifier)).build()
    }

    @PUT
    @Path("/providers/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    fun updateProvider(@PathParam("id") id: Long, @Valid request: UpdateProviderRequest): ProviderResponse {
        val provider = providerRepository.findById(id) ?: throw NotFoundException("Provider not found")
        val updated = provider.copy(name = request.name ?: provider.name, identifier = request.identifier ?: provider.identifier)
        providerRepository.update(updated)
        return ProviderResponse(updated.id!!, updated.name, updated.identifier)
    }

    @DELETE
    @Path("/providers/{id}")
    fun deleteProvider(@PathParam("id") id: Long): Response {
        providerRepository.findById(id) ?: throw NotFoundException("Provider not found")
        providerRepository.delete(id)
        return Response.noContent().build()
    }

    // ── Species Providers ──

    @POST
    @Path("/species/{id}/providers")
    @Consumes(MediaType.APPLICATION_JSON)
    fun addSpeciesProvider(@PathParam("id") id: Long, @Valid request: AddSpeciesProviderRequest) =
        speciesService.addSpeciesProviderAdmin(id, request)

    @PUT
    @Path("/species/{id}/providers/{spId}")
    @Consumes(MediaType.APPLICATION_JSON)
    fun updateSpeciesProvider(
        @PathParam("id") id: Long,
        @PathParam("spId") spId: Long,
        @Valid request: UpdateSpeciesProviderRequest
    ) = speciesService.updateSpeciesProviderAdmin(id, spId, request)

    @DELETE
    @Path("/species/{id}/providers/{spId}")
    fun deleteSpeciesProvider(@PathParam("id") id: Long, @PathParam("spId") spId: Long): Response {
        speciesService.removeSpeciesProviderAdmin(id, spId)
        return Response.noContent().build()
    }

    // ── Workflow Templates ──

    @GET
    @Path("/workflow-templates")
    fun listWorkflowTemplates(): List<AdminWorkflowTemplateResponse> =
        workflowRepository.findAllTemplates().map {
            AdminWorkflowTemplateResponse(id = it.id!!, name = it.name, orgId = it.orgId)
        }

    // ── Organizations (slim list for admin pickers) ──

    @GET
    @Path("/organizations")
    fun listOrganizations(): List<AdminOrganizationResponse> =
        organizationRepository.listAll().map { AdminOrganizationResponse(id = it.id!!, name = it.name) }

    // ── Outlets (admin-scope CRUD across all orgs) ──

    @GET
    @Path("/outlets")
    fun listOutlets(): List<AdminOutletResponse> =
        outletRepository.findAll().map {
            AdminOutletResponse(
                id = it.id!!,
                orgId = it.orgId,
                name = it.name,
                channel = it.channel.name,
                contactInfo = it.contactInfo,
                notes = it.notes,
            )
        }

    @POST
    @Path("/outlets")
    @Consumes(MediaType.APPLICATION_JSON)
    fun createOutlet(@Valid request: AdminCreateOutletRequest): Response {
        val outlet = outletRepository.persist(
            Outlet(
                orgId = request.orgId,
                name = request.name,
                channel = app.verdant.entity.Channel.valueOf(request.channel),
                contactInfo = request.contactInfo,
                notes = request.notes,
            ),
        )
        return Response.status(Response.Status.CREATED).entity(
            AdminOutletResponse(
                id = outlet.id!!,
                orgId = outlet.orgId,
                name = outlet.name,
                channel = outlet.channel.name,
                contactInfo = outlet.contactInfo,
                notes = outlet.notes,
            ),
        ).build()
    }

    @PUT
    @Path("/outlets/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    fun updateOutlet(@PathParam("id") id: Long, @Valid request: AdminUpdateOutletRequest): AdminOutletResponse {
        val outlet = outletRepository.findById(id) ?: throw NotFoundException("Outlet not found")
        val updated = outlet.copy(
            name = request.name ?: outlet.name,
            channel = request.channel?.let { app.verdant.entity.Channel.valueOf(it) } ?: outlet.channel,
            contactInfo = request.contactInfo ?: outlet.contactInfo,
            notes = request.notes ?: outlet.notes,
        )
        outletRepository.update(updated)
        return AdminOutletResponse(
            id = updated.id!!,
            orgId = updated.orgId,
            name = updated.name,
            channel = updated.channel.name,
            contactInfo = updated.contactInfo,
            notes = updated.notes,
        )
    }

    @DELETE
    @Path("/outlets/{id}")
    fun deleteOutlet(@PathParam("id") id: Long): Response {
        outletRepository.findById(id) ?: throw NotFoundException("Outlet not found")
        try {
            outletRepository.delete(id)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("foreign key", ignoreCase = true) || msg.contains("23503")) {
                throw WebApplicationException(
                    "Outlet is referenced by existing sales or active lots and cannot be deleted",
                    409,
                )
            }
            throw e
        }
        return Response.noContent().build()
    }
}

data class AdminOrganizationResponse(val id: Long, val name: String)

data class AdminOutletResponse(
    val id: Long,
    val orgId: Long,
    val name: String,
    val channel: String,
    val contactInfo: String?,
    val notes: String?,
)

data class AdminCreateOutletRequest(
    @field:jakarta.validation.constraints.NotNull
    val orgId: Long,
    @field:jakarta.validation.constraints.NotBlank @field:jakarta.validation.constraints.Size(max = 200)
    val name: String,
    @field:jakarta.validation.constraints.NotBlank
    val channel: String,
    @field:jakarta.validation.constraints.Size(max = 2000)
    val contactInfo: String? = null,
    @field:jakarta.validation.constraints.Size(max = 2000)
    val notes: String? = null,
)

data class AdminUpdateOutletRequest(
    @field:jakarta.validation.constraints.Size(max = 200)
    val name: String? = null,
    val channel: String? = null,
    @field:jakarta.validation.constraints.Size(max = 2000)
    val contactInfo: String? = null,
    @field:jakarta.validation.constraints.Size(max = 2000)
    val notes: String? = null,
)

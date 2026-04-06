package app.verdant.resource

import app.verdant.dto.*
import app.verdant.filter.OrgContext
import app.verdant.service.WorkflowService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/workflows")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class WorkflowResource(
    private val workflowService: WorkflowService,
    private val orgContext: OrgContext,
) {

    // ── Templates ──

    @GET
    @Path("/templates")
    fun listTemplates() = workflowService.getTemplates(orgContext.orgId)

    @POST
    @Path("/templates")
    fun createTemplate(@Valid request: CreateWorkflowTemplateRequest): Response {
        val template = workflowService.createTemplate(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(template).build()
    }

    @GET
    @Path("/templates/{id}")
    fun getTemplate(@PathParam("id") id: Long) = workflowService.getTemplate(id, orgContext.orgId)

    @PUT
    @Path("/templates/{id}")
    fun updateTemplate(@PathParam("id") id: Long, @Valid request: UpdateWorkflowTemplateRequest) =
        workflowService.updateTemplate(id, request, orgContext.orgId)

    @DELETE
    @Path("/templates/{id}")
    fun deleteTemplate(@PathParam("id") id: Long): Response {
        workflowService.deleteTemplate(id, orgContext.orgId)
        return Response.noContent().build()
    }

    @POST
    @Path("/templates/{id}/steps")
    fun addTemplateStep(@PathParam("id") id: Long, @Valid request: CreateWorkflowStepRequest): Response {
        val step = workflowService.addTemplateStep(id, request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(step).build()
    }

    @PUT
    @Path("/steps/{stepId}")
    fun updateTemplateStep(@PathParam("stepId") stepId: Long, @Valid request: UpdateWorkflowStepRequest) =
        workflowService.updateTemplateStep(stepId, request, orgContext.orgId)

    @DELETE
    @Path("/steps/{stepId}")
    fun deleteTemplateStep(@PathParam("stepId") stepId: Long): Response {
        workflowService.deleteTemplateStep(stepId, orgContext.orgId)
        return Response.noContent().build()
    }

    // ── Species Workflow ──

    @POST
    @Path("/species/{speciesId}/assign")
    fun assignTemplate(@PathParam("speciesId") speciesId: Long, @Valid request: AssignWorkflowTemplateRequest): Response {
        val workflow = workflowService.assignTemplateToSpecies(speciesId, request.templateId, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(workflow).build()
    }

    @GET
    @Path("/species/{speciesId}")
    fun getSpeciesWorkflow(@PathParam("speciesId") speciesId: Long) =
        workflowService.getSpeciesWorkflow(speciesId, orgContext.orgId)

    @POST
    @Path("/species/{speciesId}/steps")
    fun addSpeciesStep(@PathParam("speciesId") speciesId: Long, @Valid request: CreateWorkflowStepRequest): Response {
        val step = workflowService.addSpeciesStep(speciesId, request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(step).build()
    }

    @PUT
    @Path("/species/{speciesId}/steps/{stepId}")
    fun updateSpeciesStep(
        @PathParam("speciesId") speciesId: Long,
        @PathParam("stepId") stepId: Long,
        @Valid request: UpdateWorkflowStepRequest,
    ) = workflowService.updateSpeciesStep(stepId, request, orgContext.orgId)

    @DELETE
    @Path("/species/{speciesId}/steps/{stepId}")
    fun deleteSpeciesStep(@PathParam("speciesId") speciesId: Long, @PathParam("stepId") stepId: Long): Response {
        workflowService.deleteSpeciesStep(stepId, orgContext.orgId)
        return Response.noContent().build()
    }

    @POST
    @Path("/species/{speciesId}/sync")
    fun syncSpeciesWorkflow(@PathParam("speciesId") speciesId: Long) =
        workflowService.syncSpeciesWithTemplate(speciesId, orgContext.orgId)

    // ── Plant Progress ──

    @GET
    @Path("/plants/{plantId}")
    fun getPlantProgress(@PathParam("plantId") plantId: Long) =
        workflowService.getPlantProgress(plantId, orgContext.orgId)

    @POST
    @Path("/species-steps/{stepId}/complete")
    fun completeStep(@PathParam("stepId") stepId: Long, @Valid request: CompleteWorkflowStepRequest) =
        workflowService.completeStep(stepId, request, orgContext.orgId)

    @GET
    @Path("/species-steps/{stepId}/plants")
    fun getPlantsAtStep(@PathParam("stepId") stepId: Long, @QueryParam("speciesId") speciesId: Long) =
        workflowService.getPlantsAtStep(speciesId, stepId, orgContext.orgId)

    @POST
    @Path("/side-branches/start")
    fun startSideBranch(@Valid request: StartSideBranchRequest): Response {
        workflowService.startSideBranch(request, orgContext.orgId)
        return Response.noContent().build()
    }
}

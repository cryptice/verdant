package app.verdant.resource

import app.verdant.dto.CreateMaintenanceRuleRequest
import app.verdant.dto.UpdateMaintenanceRuleRequest
import app.verdant.filter.OrgContext
import app.verdant.service.MaintenanceRuleService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/maintenance-rules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class MaintenanceRuleResource(
    private val ruleService: MaintenanceRuleService,
    private val orgContext: OrgContext,
) {
    @GET
    fun list(@QueryParam("bedId") bedId: Long?, @QueryParam("areaId") areaId: Long?) =
        ruleService.listRules(bedId, areaId, orgContext.orgId)

    @POST
    fun create(@Valid request: CreateMaintenanceRuleRequest): Response {
        val rule = ruleService.createRule(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(rule).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateMaintenanceRuleRequest) =
        ruleService.updateRule(id, request, orgContext.orgId)

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        ruleService.deleteRule(id, orgContext.orgId)
        return Response.noContent().build()
    }
}

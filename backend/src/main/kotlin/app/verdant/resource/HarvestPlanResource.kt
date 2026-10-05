package app.verdant.resource

import app.verdant.dto.*
import app.verdant.filter.OrgContext
import app.verdant.service.HarvestPlanService
import io.quarkus.security.Authenticated
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType

@Path("/api/harvest-plans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class HarvestPlanResource(private val service: HarvestPlanService, private val org: OrgContext) {
    @GET fun list(@QueryParam("limit") @DefaultValue("50") limit: Int, @QueryParam("offset") @DefaultValue("0") offset: Int) = service.list(org.orgId, limit, offset)
    @GET @Path("/{id}") fun get(@PathParam("id") id: Long) = service.get(org.orgId, id)
    @GET @Path("/species") fun species(@QueryParam("speciesId") speciesId: Long?, @QueryParam("groupId") groupId: Long?) = service.candidates(org.orgId, speciesId, groupId)
    @PUT @Path("/species/{id}") fun profile(@PathParam("id") id: Long, request: ProductionProfile) = service.saveProfile(org.orgId, id, request)
    @POST @Path("/preview") fun preview(request: HarvestPlanRequest) = service.preview(org.orgId, request)
    @POST fun create(request: CreateHarvestPlanRequest) = service.create(org.orgId, request)
    @POST @Path("/{id}/cancel") fun cancel(@PathParam("id") id: Long) = service.cancel(org.orgId, id)
    @POST @Path("/{id}/tasks/{taskId}/complete") fun complete(@PathParam("id") id: Long, @PathParam("taskId") taskId: Long, request: CompleteHarvestPlanTaskRequest) = service.complete(org.orgId, id, taskId, request)
}

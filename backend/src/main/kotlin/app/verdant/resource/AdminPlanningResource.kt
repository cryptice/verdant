package app.verdant.resource

import app.verdant.dto.*
import app.verdant.service.AdminPlanningService
import jakarta.annotation.security.RolesAllowed
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/admin/planning")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
class AdminPlanningResource(private val service: AdminPlanningService) {
    @GET @Path("/schedules") fun schedules() = service.listSchedules()
    @GET @Path("/schedules/{key}") fun schedule(@PathParam("key") key: String) = service.schedule(key)
    @POST @Path("/schedules") fun createSchedule(request: SharedSchedule) =
        Response.status(201).entity(service.createSchedule(request)).build()
    @PUT @Path("/schedules/{key}") fun updateSchedule(@PathParam("key") key: String, request: SharedSchedule) = service.updateSchedule(key, request)
    @DELETE @Path("/schedules/{key}") fun deleteSchedule(@PathParam("key") key: String, @QueryParam("revision") revision: Int): Response {
        service.deleteSchedule(key, revision)
        return Response.noContent().build()
    }
    @GET @Path("/species") fun species() = service.planningSpecies()
    @PUT @Path("/assignments") fun assign(request: ScheduleAssignmentRequest): Response {
        service.assign(request)
        return Response.noContent().build()
    }
    @GET @Path("/groups") fun groups() = service.listGroups()
    @POST @Path("/groups") fun createGroup(request: CreateSpeciesGroupRequest) =
        Response.status(201).entity(service.saveGroup(null, request.name)).build()
    @PUT @Path("/groups/{id}") fun updateGroup(@PathParam("id") id: Long, request: CreateSpeciesGroupRequest) = service.saveGroup(id, request.name)
    @DELETE @Path("/groups/{id}") fun deleteGroup(@PathParam("id") id: Long): Response {
        service.deleteGroup(id)
        return Response.noContent().build()
    }
    @POST @Path("/groups/{id}/members") fun addMembers(@PathParam("id") id: Long, request: GroupMembersRequest): Response {
        service.addMembers(id, request)
        return Response.noContent().build()
    }
    @DELETE @Path("/groups/{id}/members/{speciesId}") fun removeMember(@PathParam("id") id: Long, @PathParam("speciesId") speciesId: Long): Response {
        service.removeMember(id, speciesId)
        return Response.noContent().build()
    }
}

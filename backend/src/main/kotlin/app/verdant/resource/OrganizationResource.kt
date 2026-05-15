package app.verdant.resource

import app.verdant.dto.*
import app.verdant.repository.UserRepository
import app.verdant.service.OrganizationService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/organizations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class OrganizationResource(
    private val organizationService: OrganizationService,
    private val jwt: JsonWebToken,
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    fun list() = organizationService.getOrganizationsForUser(userId())

    @POST
    fun create(@Valid request: CreateOrganizationRequest): Response {
        val org = organizationService.createOrganization(request, userId())
        return Response.status(Response.Status.CREATED).entity(org).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateOrganizationRequest) =
        organizationService.updateOrganization(id, request, userId())

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        organizationService.deleteOrganization(id, userId())
        return Response.noContent().build()
    }

    @GET
    @Path("/{id}/members")
    fun listMembers(@PathParam("id") id: Long) =
        organizationService.getMembers(id)

    @POST
    @Path("/{id}/invite")
    fun inviteMember(@PathParam("id") id: Long, @Valid request: InviteMemberRequest): Response {
        val invite = organizationService.inviteMember(id, request, userId())
        return Response.status(Response.Status.CREATED).entity(invite).build()
    }

    @GET
    @Path("/lookup")
    fun lookup(@QueryParam("name") name: String?): Response {
        if (name.isNullOrBlank()) throw BadRequestException("name is required")
        val org = organizationService.lookupByName(name) ?: throw NotFoundException("Organization not found")
        return Response.ok(org).build()
    }

    @POST
    @Path("/{id}/join-requests")
    fun requestJoin(@PathParam("id") id: Long): Response {
        val req = organizationService.requestJoin(id, userId())
        return Response.status(Response.Status.CREATED).entity(req).build()
    }

    @GET
    @Path("/{id}/join-requests")
    fun listJoinRequests(@PathParam("id") id: Long) =
        organizationService.getPendingJoinRequests(id, userId())

    @POST
    @Path("/{id}/join-requests/{reqId}/accept")
    fun acceptJoinRequest(@PathParam("id") id: Long, @PathParam("reqId") reqId: Long) =
        organizationService.acceptJoinRequest(id, reqId, userId())

    @POST
    @Path("/{id}/join-requests/{reqId}/decline")
    fun declineJoinRequest(@PathParam("id") id: Long, @PathParam("reqId") reqId: Long): Response {
        organizationService.declineJoinRequest(id, reqId, userId())
        return Response.noContent().build()
    }

    @GET
    @Path("/{id}/invites")
    fun listInvites(@PathParam("id") id: Long) =
        organizationService.getPendingInvitesForOrg(id, userId())

    @DELETE
    @Path("/{id}/invites/{inviteId}")
    fun cancelInvite(@PathParam("id") id: Long, @PathParam("inviteId") inviteId: Long): Response {
        organizationService.cancelInvite(id, inviteId, userId())
        return Response.noContent().build()
    }

    @DELETE
    @Path("/{id}/members/{userId}")
    fun removeMember(@PathParam("id") id: Long, @PathParam("userId") targetUserId: Long): Response {
        organizationService.removeMember(id, targetUserId, userId())
        return Response.noContent().build()
    }
}

@Path("/api/invites")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class InviteResource(
    private val organizationService: OrganizationService,
    private val jwt: JsonWebToken,
    private val userRepository: UserRepository,
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    fun listPending(): List<OrgInviteResponse> {
        val user = userRepository.findById(userId()) ?: throw NotFoundException("User not found")
        return organizationService.getPendingInvitesForUser(user.email)
    }

    @POST
    @Path("/{id}/accept")
    fun accept(@PathParam("id") id: Long): OrganizationResponse =
        organizationService.acceptInvite(id, userId())

    @POST
    @Path("/{id}/decline")
    fun decline(@PathParam("id") id: Long): Response {
        organizationService.declineInvite(id, userId())
        return Response.noContent().build()
    }
}

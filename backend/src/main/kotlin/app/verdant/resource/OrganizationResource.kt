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

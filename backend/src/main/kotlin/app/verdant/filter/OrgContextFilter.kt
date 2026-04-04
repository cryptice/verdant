package app.verdant.filter

import app.verdant.repository.OrgMemberRepository
import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.jwt.JsonWebToken

@Provider
@Priority(Priorities.AUTHORIZATION + 1)
class OrgContextFilter(
    private val jwt: JsonWebToken,
    private val orgMemberRepository: OrgMemberRepository,
    private val orgContext: OrgContext,
) : ContainerRequestFilter {

    private val exemptPrefixes = listOf(
        "/api/auth/",
        "/api/users/me",
        "/api/organizations",
        "/api/invites",
        "/api/admin/",
        "/api/dev/",
        "/api/openapi",
        "/api/swagger-ui",
        "/q/",
    )

    override fun filter(requestContext: ContainerRequestContext) {
        val path = requestContext.uriInfo.path

        if (exemptPrefixes.any { path.startsWith(it) }) return

        val subject = jwt.subject ?: return

        val userId = subject.toLongOrNull() ?: return

        val orgIdHeader = requestContext.getHeaderString("X-Organization-Id")
        if (orgIdHeader.isNullOrBlank()) {
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(mapOf("message" to "Missing X-Organization-Id header"))
                    .build()
            )
            return
        }

        val orgId = orgIdHeader.toLongOrNull()
        if (orgId == null) {
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(mapOf("message" to "Invalid X-Organization-Id header"))
                    .build()
            )
            return
        }

        if (!orgMemberRepository.isMember(orgId, userId)) {
            requestContext.abortWith(
                Response.status(Response.Status.FORBIDDEN)
                    .entity(mapOf("message" to "Not a member of the specified organization"))
                    .build()
            )
            return
        }

        orgContext.orgId = orgId
        orgContext.userId = userId
    }
}

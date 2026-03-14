package app.verdant.resource

import app.verdant.repository.UserRepository
import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.jwt.JsonWebToken

@Provider
@Priority(Priorities.AUTHORIZATION + 1)
class UserExistsFilter(
    private val jwt: JsonWebToken,
    private val userRepository: UserRepository,
) : ContainerRequestFilter {

    override fun filter(requestContext: ContainerRequestContext) {
        val subject = jwt.subject ?: return // no JWT present, let Quarkus security handle it
        val userId = subject.toLongOrNull() ?: return
        if (userRepository.findById(userId) == null) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ErrorMapper.ErrorResponse("User no longer exists", 401))
                    .build()
            )
        }
    }
}

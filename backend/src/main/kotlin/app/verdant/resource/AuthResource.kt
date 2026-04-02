package app.verdant.resource

import app.verdant.dto.AdminLoginRequest
import app.verdant.dto.GoogleAuthRequest
import app.verdant.service.AuthService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class AuthResource(private val authService: AuthService) {

    @POST
    @Path("/google")
    fun googleAuth(@Valid request: GoogleAuthRequest) = authService.authenticateWithGoogle(request.idToken)

    @POST
    @Path("/admin")
    fun adminLogin(@Valid request: AdminLoginRequest) = authService.authenticateAdmin(request.email, request.password)
}

package app.verdant.resource

import app.verdant.service.DashboardService
import io.quarkus.security.Authenticated
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
class DashboardResource(
    private val dashboardService: DashboardService,
    private val jwt: JsonWebToken
) {
    @GET
    fun getDashboard() = dashboardService.getDashboard(jwt.subject.toLong())
}

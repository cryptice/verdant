package app.verdant.resource

import app.verdant.filter.OrgContext
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
    private val orgContext: OrgContext,
    private val jwt: JsonWebToken
) {
    @GET
    fun getDashboard() = dashboardService.getDashboard(orgContext.orgId, jwt.subject.toLong())
}

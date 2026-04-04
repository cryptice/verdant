package app.verdant.resource

import app.verdant.dto.*
import app.verdant.filter.OrgContext
import app.verdant.service.CustomerService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class CustomerResource(
    private val service: CustomerService,
    private val orgContext: OrgContext
) {
    @GET
    fun list(
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = service.getCustomersForUser(orgContext.orgId, limit.coerceIn(1, 200), offset.coerceAtLeast(0))

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getCustomer(id, orgContext.orgId)

    @POST
    fun create(@Valid request: CreateCustomerRequest): Response {
        val customer = service.createCustomer(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(customer).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateCustomerRequest) =
        service.updateCustomer(id, request, orgContext.orgId)

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteCustomer(id, orgContext.orgId)
        return Response.noContent().build()
    }
}

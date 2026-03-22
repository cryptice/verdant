package app.verdant.resource

import app.verdant.dto.*
import app.verdant.service.CustomerService
import io.quarkus.security.Authenticated
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class CustomerResource(
    private val service: CustomerService,
    private val jwt: JsonWebToken
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    fun list() = service.getCustomersForUser(userId())

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getCustomer(id, userId())

    @POST
    fun create(request: CreateCustomerRequest): Response {
        val customer = service.createCustomer(request, userId())
        return Response.status(Response.Status.CREATED).entity(customer).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, request: UpdateCustomerRequest) =
        service.updateCustomer(id, request, userId())

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteCustomer(id, userId())
        return Response.noContent().build()
    }
}

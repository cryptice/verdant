package app.verdant.resource

import app.verdant.dto.*
import app.verdant.filter.OrgContext
import app.verdant.service.MarketOrderService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/market/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class MarketOrderResource(
    private val service: MarketOrderService,
    private val orgContext: OrgContext,
) {
    @POST
    fun place(@Valid request: CreateMarketOrderRequest): Response {
        val order = service.placeOrder(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(order).build()
    }

    @GET
    @Path("/mine")
    fun mine(
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = service.getOrdersForPurchaser(orgContext.orgId, limit.coerceIn(1, 200), offset.coerceAtLeast(0))

    @GET
    @Path("/incoming")
    fun incoming(
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = service.getOrdersForProducer(orgContext.orgId, limit.coerceIn(1, 200), offset.coerceAtLeast(0))

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getOrderForUser(id, orgContext.orgId)

    @PUT
    @Path("/{id}/status")
    fun updateStatus(@PathParam("id") id: Long, @Valid request: UpdateOrderStatusRequest) =
        service.updateOrderStatus(id, request, orgContext.orgId)
}

package app.verdant.resource

import app.verdant.dto.*
import app.verdant.filter.OrgContext
import app.verdant.service.BouquetRecipeService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/bouquet-recipes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class BouquetRecipeResource(
    private val service: BouquetRecipeService,
    private val orgContext: OrgContext
) {
    @GET
    fun list(
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ) = service.getRecipesForUser(orgContext.orgId, limit.coerceIn(1, 200), offset.coerceAtLeast(0))

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getRecipe(id, orgContext.orgId)

    @POST
    fun create(@Valid request: CreateBouquetRecipeRequest): Response {
        val recipe = service.createRecipe(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(recipe).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateBouquetRecipeRequest) =
        service.updateRecipe(id, request, orgContext.orgId)

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteRecipe(id, orgContext.orgId)
        return Response.noContent().build()
    }
}

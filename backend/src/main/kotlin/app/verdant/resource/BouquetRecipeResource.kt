package app.verdant.resource

import app.verdant.dto.*
import app.verdant.service.BouquetRecipeService
import io.quarkus.security.Authenticated
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/bouquet-recipes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class BouquetRecipeResource(
    private val service: BouquetRecipeService,
    private val jwt: JsonWebToken
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    fun list() = service.getRecipesForUser(userId())

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: Long) = service.getRecipe(id, userId())

    @POST
    fun create(request: CreateBouquetRecipeRequest): Response {
        val recipe = service.createRecipe(request, userId())
        return Response.status(Response.Status.CREATED).entity(recipe).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, request: UpdateBouquetRecipeRequest) =
        service.updateRecipe(id, request, userId())

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        service.deleteRecipe(id, userId())
        return Response.noContent().build()
    }
}

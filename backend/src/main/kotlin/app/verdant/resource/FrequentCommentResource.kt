package app.verdant.resource

import app.verdant.dto.FrequentCommentResponse
import app.verdant.dto.RecordCommentRequest
import app.verdant.repository.FrequentCommentRepository
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class FrequentCommentResource(
    private val repo: FrequentCommentRepository,
    private val jwt: JsonWebToken
) {
    private fun userId() = jwt.subject.toLong()

    @GET
    fun list() = repo.findByUserId(userId()).map {
        FrequentCommentResponse(it.id!!, it.text, it.useCount)
    }

    @POST
    fun record(@Valid request: RecordCommentRequest): Response {
        val comment = repo.recordUsage(userId(), request.text)
        return Response.status(Response.Status.CREATED)
            .entity(FrequentCommentResponse(comment.id!!, comment.text, comment.useCount))
            .build()
    }

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        repo.delete(id)
        return Response.noContent().build()
    }
}

package app.verdant.resource

import app.verdant.auth.TokenService
import app.verdant.entity.*
import app.verdant.mcp.McpTestProfile
import app.verdant.repository.*
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID

/** An organization may put its own varieties in a catalogue group; other organizations never see them there. */
@QuarkusTest
@TestProfile(McpTestProfile::class)
class CatalogueGroupMembershipTest {
    @Inject lateinit var users: UserRepository
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var members: OrgMemberRepository
    @Inject lateinit var species: SpeciesRepository
    @Inject lateinit var groups: SpeciesGroupRepository
    @Inject lateinit var tokens: TokenService
    @Inject lateinit var mapper: ObjectMapper
    @TestHTTPResource lateinit var base: URI
    private val http = HttpClient.newHttpClient()

    private data class Grower(val orgId: Long, val token: String)

    private fun grower(): Grower {
        val user = users.persist(User(email = "group-${UUID.randomUUID()}@example.test", displayName = "Grower", role = Role.USER))
        val orgId = orgs.persist(Organization(name = "Group org")).id!!
        members.persist(OrgMember(orgId = orgId, userId = user.id!!))
        return Grower(orgId, tokens.generateToken(user))
    }

    private fun send(g: Grower, path: String, body: String? = null, method: String = if (body == null) "GET" else "POST"): HttpResponse<String> =
        http.send(
            HttpRequest.newBuilder(base.resolve(path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer ${g.token}")
                .header("X-Organization-Id", g.orgId.toString())
                .method(method, body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )

    @Test fun `own varieties join a catalogue group without showing up for anyone else`() {
        val a = grower()
        val b = grower()
        val dahlior = groups.persist(SpeciesGroup(name = "Dahlior ${UUID.randomUUID()}")).id!!
        val catalogueDahlia = species.persist(Species(commonName = "Catalogue dahlia")).id!!
        groups.addSpeciesToGroup(catalogueDahlia, dahlior)
        val aDahlia = species.persist(Species(commonName = "A's dahlia", orgId = a.orgId)).id!!
        val bDahlia = species.persist(Species(commonName = "B's dahlia", orgId = b.orgId)).id!!

        assertEquals(204, send(a, "/api/species/groups/$dahlior/species/$aDahlia", "").statusCode())
        assertEquals(403, send(a, "/api/species/groups/$dahlior/species/$catalogueDahlia", "").statusCode(), "the catalogue's own membership is the admins'")
        assertEquals(404, send(a, "/api/species/groups/$dahlior/species/$bDahlia", "").statusCode(), "another organization's variety")
        assertEquals(403, send(b, "/api/species/groups/$dahlior/species/$aDahlia", method = "DELETE").statusCode())

        val aSees = mapper.readTree(send(a, "/api/species/groups/$dahlior/species").body()).map { it["id"].asLong() }.toSet()
        val bSees = mapper.readTree(send(b, "/api/species/groups/$dahlior/species").body()).map { it["id"].asLong() }.toSet()
        assertEquals(setOf(catalogueDahlia, aDahlia), aSees)
        assertEquals(setOf(catalogueDahlia), bSees)

        val task = send(b, "/api/tasks", """{"activityType":"SOW","speciesGroupId":$dahlior,"deadline":"2027-03-01","targetCount":10}""")
        assertEquals(201, task.statusCode(), task.body())
        val acceptable = mapper.readTree(task.body())["acceptableSpecies"].map { it["speciesId"].asLong() }.toSet()
        assertEquals(setOf(catalogueDahlia), acceptable)

        val listed = mapper.readTree(send(a, "/api/species/groups").body()).single { it["id"].asLong() == dahlior }
        assertTrue(listed["isSystem"].asBoolean())
    }

    @Test fun `a removed own variety leaves the catalogue group`() {
        val a = grower()
        val group = groups.persist(SpeciesGroup(name = "Zinnior ${UUID.randomUUID()}")).id!!
        val own = species.persist(Species(commonName = "A's zinnia", orgId = a.orgId)).id!!
        send(a, "/api/species/groups/$group/species/$own", "")
        assertEquals(204, send(a, "/api/species/groups/$group/species/$own", method = "DELETE").statusCode())
        assertTrue(mapper.readTree(send(a, "/api/species/groups/$group/species").body()).none { it["id"].asLong() == own })
    }
}

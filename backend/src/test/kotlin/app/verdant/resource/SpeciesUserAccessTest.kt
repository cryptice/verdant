package app.verdant.resource

import app.verdant.auth.TokenService
import app.verdant.entity.*
import app.verdant.mcp.McpTestProfile
import app.verdant.repository.*
import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID

/** What a regular user may do from the species form: pick providers, and add photos only to their own varieties. */
@QuarkusTest
@TestProfile(McpTestProfile::class)
class SpeciesUserAccessTest {
    @Inject lateinit var users: UserRepository
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var members: OrgMemberRepository
    @Inject lateinit var species: SpeciesRepository
    @Inject lateinit var tokens: TokenService
    @TestHTTPResource lateinit var base: URI
    private val http = HttpClient.newHttpClient()
    private var orgId = 0L
    private lateinit var token: String

    @BeforeEach fun setup() {
        val user = users.persist(User(email = "species-${UUID.randomUUID()}@example.test", displayName = "Grower", role = Role.USER))
        orgId = orgs.persist(Organization(name = "Species access org")).id!!
        members.persist(OrgMember(orgId = orgId, userId = user.id!!))
        token = tokens.generateToken(user)
    }

    private fun send(path: String, body: String? = null, method: String = if (body == null) "GET" else "POST"): HttpResponse<String> {
        val request = HttpRequest.newBuilder(base.resolve(path))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $token")
            .header("X-Organization-Id", orgId.toString())
            .method(method, body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody())
            .build()
        return http.send(request, HttpResponse.BodyHandlers.ofString())
    }

    @Test fun `a user lists seed providers but cannot manage them`() {
        assertEquals(200, send("/api/providers").statusCode())
        assertEquals(403, send("/api/providers", """{"name":"Impecta","identifier":"impecta"}""").statusCode())
    }

    @Test fun `a user cannot add photos to catalogue varieties or another organization's`() {
        val catalogue = species.persist(Species(commonName = "Catalogue zinnia")).id!!
        val otherOrg = orgs.persist(Organization(name = "Other species org")).id!!
        val foreign = species.persist(Species(commonName = "Foreign zinnia", orgId = otherOrg)).id!!
        val photo = """{"imageBase64":"aGVsbG8="}"""

        assertEquals(403, send("/api/species/$catalogue/photos", photo).statusCode())
        assertEquals(404, send("/api/species/$foreign/photos", photo).statusCode())
        assertEquals(404, send("/api/species/$foreign/photos/1", method = "DELETE").statusCode())
    }
}

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
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID

/** The clients page and group these lists themselves, so without a limit they come back whole. */
@QuarkusTest
@TestProfile(McpTestProfile::class)
class FullListTest {
    @Inject lateinit var users: UserRepository
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var members: OrgMemberRepository
    @Inject lateinit var species: SpeciesRepository
    @Inject lateinit var inventory: SeedInventoryRepository
    @Inject lateinit var customers: CustomerRepository
    @Inject lateinit var tokens: TokenService
    @Inject lateinit var mapper: ObjectMapper
    @TestHTTPResource lateinit var base: URI
    private val http = HttpClient.newHttpClient()

    @Test fun `seed stock and customers come back whole without a limit, and capped with one`() {
        val user = users.persist(User(email = "lists-${UUID.randomUUID()}@example.test", displayName = "Grower", role = Role.USER))
        val orgId = orgs.persist(Organization(name = "Full list org")).id!!
        members.persist(OrgMember(orgId = orgId, userId = user.id!!))
        val token = tokens.generateToken(user)
        val zinnia = species.persist(Species(commonName = "List zinnia", orgId = orgId)).id!!
        repeat(60) {
            inventory.persist(SeedInventory(orgId = orgId, speciesId = zinnia, quantity = 10 + it))
            customers.persist(Customer(orgId = orgId, name = "Customer $it"))
        }
        val count = { path: String ->
            val response = http.send(
                HttpRequest.newBuilder(base.resolve(path)).header("Authorization", "Bearer $token").header("X-Organization-Id", orgId.toString()).build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            mapper.readTree(response.body()).size()
        }
        assertEquals(60, count("/api/seed-stock"))
        assertEquals(60, count("/api/customers"))
        assertEquals(10, count("/api/seed-stock?limit=10"))
    }
}

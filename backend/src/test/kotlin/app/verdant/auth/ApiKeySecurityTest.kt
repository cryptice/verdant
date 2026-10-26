package app.verdant.auth

import app.verdant.entity.*
import app.verdant.mcp.McpTestProfile
import app.verdant.repository.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.agroal.api.AgroalDataSource
import io.smallrye.jwt.build.Jwt
import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID

@QuarkusTest
@TestProfile(McpTestProfile::class)
class ApiKeySecurityTest {
    @Inject lateinit var ds: AgroalDataSource
    @Inject lateinit var users: UserRepository
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var members: OrgMemberRepository
    @Inject lateinit var seasons: SeasonRepository
    @Inject lateinit var tokens: TokenService
    @Inject lateinit var mapper: ObjectMapper
    @TestHTTPResource lateinit var base: URI
    private val http = HttpClient.newHttpClient()
    private lateinit var user: User
    private var orgId = 0L
    private lateinit var appToken: String

    @BeforeEach fun setup() {
        user = users.persist(User(email = "key-${UUID.randomUUID()}@example.test", displayName = "Key test", role = Role.ADMIN))
        orgId = orgs.persist(Organization(name = "Key organization")).id!!
        members.persist(OrgMember(orgId = orgId, userId = user.id!!))
        appToken = tokens.generateToken(user)
    }

    private fun send(path: String, token: String, body: String? = null, org: Long? = orgId, method: String = if (body == null) "GET" else "POST"): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(base.resolve(path)).header("Content-Type", "application/json").header("Authorization", "Bearer $token")
        org?.let { builder.header("X-Organization-Id", it.toString()) }
        return http.send(builder.method(method, body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString())
    }
    private fun createKey(token: String = appToken, validDays: Int = 90): JsonNode {
        val response = send("/api/api-keys", token, mapper.writeValueAsString(mapOf("name" to "Claude Code", "validDays" to validDays)))
        assertEquals(201, response.statusCode(), response.body())
        assertEquals("no-store", response.headers().firstValue("Cache-Control").orElse(null))
        return mapper.readTree(response.body())
    }

    @Test fun `a key reads and writes its organization's data without an organization header`() {
        seasons.persist(Season(orgId = orgId, name = "Key season", year = 2027))
        val key = createKey()
        val token = key.path("token").asText()
        assertEquals(orgId, key.path("key").path("organizationId").asLong())

        val read = send("/api/seasons", token, org = null)
        assertEquals(200, read.statusCode(), read.body())
        assertTrue(read.body().contains("Key season"))
        assertEquals(200, send("/api/seasons", token).statusCode())

        val created = send("/api/workflows/templates", token, mapper.writeValueAsString(mapOf("name" to "Dahlia från frö")), org = null)
        assertTrue(created.statusCode() in 200..201, created.body())

        val listed = mapper.readTree(send("/api/api-keys", appToken).body())
        assertEquals(1, listed.size())
        assertFalse(listed[0].has("token"))
        assertFalse(listed[0].path("lastUsedAt").isNull)
    }

    @Test fun `a key is refused for another organization and outside organization data`() {
        val other = orgs.persist(Organization(name = "Other key org")).id!!
        members.persist(OrgMember(orgId = other, userId = user.id!!))
        val token = createKey().path("token").asText()

        assertEquals(403, send("/api/seasons", token, org = other).statusCode())
        for (path in listOf("/api/api-keys", "/api/users/me", "/api/organizations", "/api/admin/users"))
            assertEquals(403, send(path, token).statusCode(), path)
        assertEquals(403, send("/api/organizations", token, mapper.writeValueAsString(mapOf("name" to "Key-made org"))).statusCode())
        assertEquals(403, send("/api/api-keys", token, mapper.writeValueAsString(mapOf("name" to "Minted"))).statusCode())
        assertEquals(401, send("/mcp", token, """{"jsonrpc":"2.0","id":1,"method":"tools/list"}""").statusCode())
    }

    @Test fun `revoked, expired and former member keys stop working`() {
        val revoked = createKey()
        val expired = createKey()
        val live = createKey().path("token").asText()
        assertEquals(204, send("/api/api-keys/${revoked.path("key").path("id").asText()}", appToken, method = "DELETE").statusCode())
        assertEquals(401, send("/api/seasons", revoked.path("token").asText()).statusCode())

        ds.connection.use { c -> c.prepareStatement("UPDATE api_key SET expires_at = now() - interval '1 second' WHERE id = ?").use { p ->
            p.setObject(1, UUID.fromString(expired.path("key").path("id").asText())); p.executeUpdate() } }
        assertEquals(401, send("/api/seasons", expired.path("token").asText()).statusCode())
        val listedIds = mapper.readTree(send("/api/api-keys", appToken).body()).map { it.path("id").asText() }
        assertFalse(revoked.path("key").path("id").asText() in listedIds)
        assertFalse(expired.path("key").path("id").asText() in listedIds)

        assertEquals(200, send("/api/seasons", live).statusCode())
        ds.connection.use { c -> c.prepareStatement("DELETE FROM org_member WHERE org_id = ? AND user_id = ?").use { p ->
            p.setLong(1, orgId); p.setLong(2, user.id!!); p.executeUpdate() } }
        assertEquals(401, send("/api/seasons", live).statusCode())
    }

    @Test fun `only the owner can revoke a key, and lifetime is bounded`() {
        val key = createKey()
        val stranger = users.persist(User(email = "stranger-${UUID.randomUUID()}@example.test", displayName = "Stranger"))
        members.persist(OrgMember(orgId = orgId, userId = stranger.id!!))
        val strangerToken = tokens.generateToken(stranger)
        assertEquals(404, send("/api/api-keys/${key.path("key").path("id").asText()}", strangerToken, method = "DELETE").statusCode())
        assertEquals(0, mapper.readTree(send("/api/api-keys", strangerToken).body()).size())
        assertEquals(200, send("/api/seasons", key.path("token").asText()).statusCode())

        for (days in listOf(0, 366))
            assertEquals(400, send("/api/api-keys", appToken, mapper.writeValueAsString(mapOf("name" to "Bad", "validDays" to days))).statusCode(), "$days")
        assertEquals(400, send("/api/api-keys", appToken, mapper.writeValueAsString(mapOf("name" to " "))).statusCode())
    }

    @Test fun `a key cannot reach account, consent, invite or admin-role endpoints`() {
        val token = createKey().path("token").asText()
        assertEquals(403, send("/api/providers", token).statusCode())
        assertEquals(403, send("/api/mcp/oauth/connections", token).statusCode())
        assertEquals(403, send("/api/invites", token).statusCode())
        assertEquals(403, send("/api/users/me/export", token).statusCode())
        assertEquals(403, send("/api/users/me", token, method = "DELETE").statusCode())
        assertTrue(send("/api;x=1/users/me", token).statusCode() in setOf(403, 404))
        assertNotNull(users.findById(user.id!!))
    }

    @Test fun `tokens with mismatched or missing key claims are refused`() {
        val key = createKey().path("key")
        val id = key.path("id").asText()
        fun forged(subject: String, org: Any?, keyId: String?) = Jwt.issuer("verdant-api").subject(subject).groups(setOf("USER"))
            .claim("token_type", "api_key").also { b -> org?.let { b.claim("api_org", it) }; keyId?.let { b.claim("api_key", it) } }
            .expiresIn(java.time.Duration.ofHours(1)).sign()
        assertEquals(200, send("/api/seasons", forged(user.id.toString(), orgId, id)).statusCode())
        assertEquals(401, send("/api/seasons", forged(user.id.toString(), orgId + 1, id)).statusCode())
        assertEquals(401, send("/api/seasons", forged((user.id!! + 1).toString(), orgId, id)).statusCode())
        assertEquals(401, send("/api/seasons", forged(user.id.toString(), orgId, null)).statusCode())
        assertEquals(401, send("/api/seasons", forged(user.id.toString(), orgId, "not-a-uuid")).statusCode())
        assertEquals(401, send("/api/seasons", forged(user.id.toString(), null, id)).statusCode())
    }

    @Test fun `leaving revokes keys for good and a deleted user's key stops working`() {
        val token = createKey().path("token").asText()
        members.delete(orgId, user.id!!)
        members.persist(OrgMember(orgId = orgId, userId = user.id!!))
        assertEquals(401, send("/api/seasons", token).statusCode())

        val fresh = createKey().path("token").asText()
        assertEquals(200, send("/api/seasons", fresh).statusCode())
        users.delete(user.id!!)
        assertEquals(401, send("/api/seasons", fresh).statusCode())
    }
}

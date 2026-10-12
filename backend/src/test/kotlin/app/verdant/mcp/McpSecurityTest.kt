package app.verdant.mcp

import app.verdant.auth.TokenService
import app.verdant.entity.*
import app.verdant.repository.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.quarkus.test.common.http.TestHTTPResource
import jakarta.inject.Inject
import io.agroal.api.AgroalDataSource
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyPairGenerator
import java.util.Base64
import java.util.UUID

class McpTestProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> {
        val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        fun pem(type: String, bytes: ByteArray) = "-----BEGIN $type-----\n${Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(bytes)}\n-----END $type-----"
        return mapOf("smallrye.jwt.sign.key" to pem("PRIVATE KEY", keys.private.encoded), "mp.jwt.verify.publickey" to pem("PUBLIC KEY", keys.public.encoded))
    }
}

@QuarkusTest
@TestProfile(McpTestProfile::class)
class McpSecurityTest {
    @Inject lateinit var ds: AgroalDataSource
    @Inject lateinit var service: McpOAuthService
    @Inject lateinit var repo: McpOAuthRepository
    @Inject lateinit var users: UserRepository
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var members: OrgMemberRepository
    @Inject lateinit var planning: McpPlanningRepository
    @Inject lateinit var species: SpeciesRepository
    @Inject lateinit var seasons: SeasonRepository
    @Inject lateinit var tokens: TokenService
    @Inject lateinit var mapper: ObjectMapper
    @TestHTTPResource lateinit var base: URI
    private val http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
    private lateinit var user: User
    private var orgId = 0L
    private lateinit var client: McpClient
    private lateinit var appToken: String
    private val redirect = "http://127.0.0.1:48484/callback"
    private val resource = "https://verdantplanner.com/mcp"
    private val verifier = "v".repeat(64)

    @BeforeEach fun setup() {
        user = users.persist(User(email = "mcp-${UUID.randomUUID()}@example.test", displayName = "MCP test"))
        orgId = orgs.persist(Organization(name = "MCP organization")).id!!
        members.persist(OrgMember(orgId = orgId, userId = user.id!!))
        client = service.register("Test client", listOf(redirect))
        appToken = tokens.generateToken(user)
    }
    private fun requestId(): UUID {
        val url = service.authorize(client.id, redirect, "code", McpConfig.READ_SCOPE, resource, "state", McpSecrets.challenge(verifier), "S256")
        return UUID.fromString(URI(url).query.substringAfter("request="))
    }
    private fun code(): String = URI(service.consent(requestId(), user.id!!, orgId, true)).query.split('&').first { it.startsWith("code=") }.substringAfter('=')
    private fun exchange(code: String, verification: String = verifier, target: String = resource, id: String = client.id, callback: String = redirect): HttpResponse<String> = send("/mcp/oauth/token", body = form(mapOf("grant_type" to "authorization_code", "client_id" to id, "code" to code, "redirect_uri" to callback, "code_verifier" to verification, "resource" to target)), type = "application/x-www-form-urlencoded")
    private fun access(): JsonNode = mapper.readTree(exchange(code()).also { assertEquals(200, it.statusCode(), it.body()) }.body())
    private fun rpc(token: String, name: String, arguments: Map<String, Any> = emptyMap()) = send("/mcp", token, mapper.writeValueAsString(mapOf("jsonrpc" to "2.0", "id" to 1, "method" to "tools/call", "params" to mapOf("name" to name, "arguments" to arguments))))
    private fun send(path: String, token: String? = null, body: String? = null, type: String = "application/json", origin: String? = null, method: String = if (body == null) "GET" else "POST"): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(base.resolve(path)).header("Content-Type", type)
        token?.let { builder.header("Authorization", "Bearer $it") }
        origin?.let { builder.header("Origin", it) }
        return http.send(builder.method(method, body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString())
    }
    private fun form(values: Map<String, String>) = values.entries.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, Charsets.UTF_8)}" }

    @Test fun `read tools handle empty seasons and preview without creating records`() {
        val season = seasons.persist(Season(orgId = orgId, name = "Review season", year = 2027)).id!!
        val sp = species.persist(Species(orgId = orgId, commonName = "Test dahlia", daysToHarvestMin = 90, expectedStemsPerPlant = 3, germinationRate = 80)).id!!
        val token = access().path("access_token").asText()
        for (name in listOf("get_season_review", "get_planning_gaps", "list_tasks", "list_seed_inventory")) {
            val result = mapper.readTree(rpc(token, name, mapOf("seasonId" to season)).body())
            assertFalse(result.path("result").path("isError").asBoolean(), "$name: $result")
            assertTrue(result.path("result").has("structuredContent"), "$name: $result")
        }
        for ((name, args) in listOf(
            "search_species" to mapOf("query" to "Test dahlia"),
            "get_species_details" to mapOf("speciesId" to sp),
            "get_planning_candidates" to mapOf("speciesId" to sp),
            "preview_harvest_plan" to mapOf("seasonId" to season, "speciesId" to sp, "quantity" to 100, "sellableUnit" to "STEM", "harvestDate" to java.time.LocalDate.now().plusDays(365).toString()),
        )) {
            val result = mapper.readTree(rpc(token, name, args).body())
            assertTrue(result.path("result").has("structuredContent"), "$name: $result")
        }
        assertEquals(0, mapper.readTree(rpc(token, "list_harvest_plans").body()).path("result").path("structuredContent").path("data").path("items").size())
        assertEquals(0, mapper.readTree(rpc(token, "list_tasks", mapOf("seasonId" to season)).body()).path("result").path("structuredContent").path("data").path("items").size())
    }

    @Test fun `planned stem aggregation excludes cancelled flower and other organization plans`() {
        val season = seasons.persist(Season(orgId = orgId, name = "Gap season", year = 2027)).id!!
        val other = orgs.persist(Organization(name = "Other gap org")).id!!
        val otherSeason = seasons.persist(Season(orgId = other, name = "Other season", year = 2027)).id!!
        fun insert(org: Long, seasonId: Long, unit: String, status: String, quantity: Int) {
            ds.connection.use { c -> c.prepareStatement("INSERT INTO harvest_plan(org_id,season_id,request_key,request,snapshot,status) VALUES (?,?,?,?::jsonb,?::jsonb,?)").use { p ->
                p.setLong(1, org); p.setLong(2, seasonId); p.setObject(3, UUID.randomUUID())
                p.setString(4, "{\"harvestDate\":\"2027-07-01\",\"sellableUnit\":\"$unit\"}")
                p.setString(5, "{\"allocations\":[{\"speciesId\":10,\"outputQuantity\":$quantity}]}")
                p.setString(6, status); p.executeUpdate()
            } }
        }
        insert(orgId, season, "STEM", "ACTIVE", 70)
        insert(orgId, season, "STEM", "ACTIVE", 10)
        insert(orgId, season, "FLOWER", "ACTIVE", 1000)
        insert(orgId, season, "STEM", "CANCELLED", 1000)
        insert(other, otherSeason, "STEM", "ACTIVE", 1000)
        assertEquals(listOf(PlannedStems(10, java.time.LocalDate.of(2027, 7, 1), 80)), planning.plannedStems(orgId, season))
        assertTrue(planning.plannedStems(orgId, otherSeason).isEmpty())
    }

    @Test fun `HTTP authorization requires explicit consent and expires`() {
        val registered = send("/mcp/oauth/register", body = mapper.writeValueAsString(mapOf("client_name" to "HTTP client", "redirect_uris" to listOf(redirect))))
        assertEquals(201, registered.statusCode())
        val id = mapper.readTree(registered.body()).path("client_id").asText()
        val query = form(mapOf("client_id" to id, "redirect_uri" to redirect, "response_type" to "code", "scope" to McpConfig.READ_SCOPE, "resource" to resource, "state" to "state", "code_challenge" to McpSecrets.challenge(verifier), "code_challenge_method" to "S256"))
        val authorization = send("/mcp/oauth/authorize?$query")
        assertEquals(303, authorization.statusCode())
        val request = URI(authorization.headers().firstValue("Location").orElseThrow()).query.substringAfter("request=")
        assertEquals(200, send("/api/mcp/oauth/requests/$request").statusCode())
        assertEquals(401, send("/api/mcp/oauth/requests/$request/approve", body = "{\"orgId\":$orgId}").statusCode())
        val denied = send("/api/mcp/oauth/requests/$request/deny", appToken, "{}")
        assertEquals(200, denied.statusCode(), denied.body())
        assertTrue(mapper.readTree(denied.body()).path("redirectUrl").asText().contains("error=access_denied"))
        assertEquals(404, send("/api/mcp/oauth/requests/$request").statusCode())
        val expired = McpAuthorization(UUID.randomUUID(), client.id, redirect, null, McpSecrets.challenge(verifier), java.time.Instant.now().minusSeconds(1))
        repo.createRequest(expired)
        assertEquals(404, send("/api/mcp/oauth/requests/${expired.id}").statusCode())
        assertEquals(404, send("/api/mcp/oauth/requests/${expired.id}/approve", appToken, "{\"orgId\":$orgId}").statusCode())
    }

    @Test fun `expired codes and grants cannot mint or use access`() {
        val expiredCode = code()
        ds.connection.use { c -> c.prepareStatement("UPDATE mcp_oauth_code SET expires_at=now()-interval '1 second' WHERE code_hash=?").use { p -> p.setString(1, McpSecrets.hash(expiredCode)); p.executeUpdate() } }
        assertEquals(400, exchange(expiredCode).statusCode())
        val access = access()
        ds.connection.use { c -> c.prepareStatement("UPDATE mcp_oauth_grant SET expires_at=now()-interval '1 second' WHERE user_id=?").use { p -> p.setLong(1, user.id!!); p.executeUpdate() } }
        assertEquals(401, rpc(access.path("access_token").asText(), "list_seasons").statusCode())
    }

    @Test fun `discovery unauthorized and malicious origin`() {
        assertEquals(200, send("/.well-known/oauth-protected-resource/mcp").statusCode())
        assertEquals(resource, mapper.readTree(send("/.well-known/oauth-protected-resource/mcp").body()).path("resource").asText())
        for (token in listOf(null, "invalid-token")) {
            val response = send("/mcp", token, "{}")
            assertEquals(401, response.statusCode())
            assertTrue(response.headers().firstValue("WWW-Authenticate").orElse("").contains("resource_metadata="))
        }
        assertEquals(403, send("/mcp", body = "{}", origin = "https://attacker.test").statusCode())
    }

    @Test fun `code is bound to resource redirect client and PKCE and used once`() {
        val code = code()
        assertEquals(400, exchange(code, target = "https://attacker.test/mcp").statusCode())
        assertEquals(400, exchange(code, callback = "$redirect/other").statusCode())
        val other = service.register("Other", listOf(redirect))
        assertEquals(400, exchange(code, id = other.id).statusCode())
        assertEquals(400, exchange(code, verification = "w".repeat(64)).statusCode())
        assertEquals(200, exchange(code).statusCode())
        assertEquals(400, exchange(code).statusCode())
    }

    @Test fun `organization consent membership and single use`() {
        val request = requestId()
        val other = orgs.persist(Organization(name = "Other")).id!!
        assertThrows(jakarta.ws.rs.ForbiddenException::class.java) { service.consent(request, user.id!!, other, true) }
        assertTrue(service.consent(request, user.id!!, orgId, true).contains("state=state"))
        assertThrows(jakarta.ws.rs.NotFoundException::class.java) { service.consent(request, user.id!!, orgId, true) }
    }

    @Test fun `delegated credentials only read one organization and cannot use app API`() {
        val token = access().path("access_token").asText()
        val other = orgs.persist(Organization(name = "Secret organization")).id!!
        val privateSeason = seasons.persist(Season(orgId = other, name = "Secret", year = 2027))
        assertEquals(403, send("/api/users/me", token).statusCode())
        assertEquals(403, send("/api/organizations", token, "{}").statusCode())
        assertEquals(401, rpc(appToken, "list_seasons").statusCode())
        val read = mapper.readTree(rpc(token, "get_season_review", mapOf("seasonId" to privateSeason.id!!)).body())
        assertTrue(read.path("result").path("isError").asBoolean())
        val injected = mapper.readTree(rpc(token, "list_seasons", mapOf("orgId" to other)).body())
        assertEquals(-32602, injected.path("error").path("code").asInt())
        assertFalse(rpc(token, "list_seasons").body().contains("Secret"))
        assertEquals(200, rpc(token, "get_connection_profile").statusCode())
        assertTrue(mapper.readTree(rpc(token, "get_connection_profile").body()).path("result").path("structuredContent").path("id").asText().isNotBlank())
        members.delete(orgId, user.id!!)
        assertEquals(401, rpc(token, "list_seasons").statusCode())
    }

    @Test fun `refresh rotates and replay revokes all access`() {
        val first = access()
        fun refresh(value: String) = send("/mcp/oauth/token", body = form(mapOf("grant_type" to "refresh_token", "client_id" to client.id, "refresh_token" to value, "resource" to resource)), type = "application/x-www-form-urlencoded")
        val secondResponse = refresh(first.path("refresh_token").asText())
        assertEquals(200, secondResponse.statusCode(), secondResponse.body())
        val second = mapper.readTree(secondResponse.body())
        assertNotEquals(first.path("refresh_token").asText(), second.path("refresh_token").asText())
        assertEquals(400, refresh(first.path("refresh_token").asText()).statusCode())
        assertEquals(401, rpc(second.path("access_token").asText(), "list_seasons").statusCode())
        assertEquals(400, refresh(second.path("refresh_token").asText()).statusCode())
    }

    @Test fun `owner revocation immediately denies delegated access`() {
        val access = access().path("access_token").asText()
        val connection = mapper.readTree(send("/api/mcp/oauth/connections", appToken).body()).first().path("id").asText()
        assertEquals(204, send("/api/mcp/oauth/connections/$connection", appToken, method = "DELETE").statusCode())
        assertEquals(401, rpc(access, "list_seasons").statusCode())
    }

    @Test fun `streamable protocol initialization tool discovery and notification`() {
        val token = access().path("access_token").asText()
        val init = send("/mcp", token, """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18"}}""")
        assertEquals("2025-06-18", mapper.readTree(init.body()).path("result").path("protocolVersion").asText())
        assertEquals(405, send("/mcp", token).statusCode())
        assertEquals(202, send("/mcp", token, """{"jsonrpc":"2.0","method":"notifications/initialized"}""").statusCode())
        val list = mapper.readTree(send("/mcp", token, """{"jsonrpc":"2.0","id":2,"method":"tools/list"}""").body())
        assertTrue(list.path("result").path("tools").all { it.path("annotations").path("readOnlyHint").asBoolean() })
        val bad = mapper.readTree(rpc(token, "list_tasks", mapOf("seasonId" to 1, "limit" to 101)).body())
        assertEquals(-32602, bad.path("error").path("code").asInt())
    }
}

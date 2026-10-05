package app.verdant.resource

import app.verdant.dto.SharedSchedule
import app.verdant.entity.Role
import app.verdant.entity.User
import app.verdant.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.smallrye.jwt.build.Jwt
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyPairGenerator
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.Base64
import java.util.UUID

class PlanningApiProfile : QuarkusTestProfile {
    override fun getConfigOverrides() = mapOf(
        "mp.jwt.verify.issuer" to "verdant-api",
        "mp.jwt.verify.publickey" to "-----BEGIN PUBLIC KEY-----\n${Base64.getEncoder().encodeToString(keys.public.encoded)}\n-----END PUBLIC KEY-----",
        "smallrye.jwt.sign.key" to "-----BEGIN PRIVATE KEY-----\n${Base64.getEncoder().encodeToString(keys.private.encoded)}\n-----END PRIVATE KEY-----",
    )
    companion object { val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair() }
}

@QuarkusTest
@TestProfile(PlanningApiProfile::class)
class AdminPlanningResourceTest {
    @TestHTTPResource lateinit var base: URI
    @Inject lateinit var mapper: ObjectMapper
    @Inject lateinit var users: UserRepository
    @ConfigProperty(name = "smallrye.jwt.sign.key") lateinit var signingKey: java.util.Optional<String>
    private val ids = mutableMapOf<String, Long>()
    private val http = HttpClient.newHttpClient()
    private fun request(path: String, method: String = "GET", role: String? = "ADMIN", body: Any? = null): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(base.resolve("/api/admin/planning/$path")).header("Content-Type", "application/json")
        if (role != null) {
            val id = ids.getOrPut(role) { users.persist(User(email = "${UUID.randomUUID()}@example.test", displayName = "Planning API test", role = Role.valueOf(role))).id!! }
            val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(signingKey.orElseThrow().replace(Regex("-----[^-]+-----|\\s"), ""))))
            builder.header("Authorization", "Bearer " + Jwt.issuer("verdant-api").subject(id.toString()).claim("userId", id).groups(setOf(role)).sign(key))
        }
        return http.send(builder.method(method, if (body == null) HttpRequest.BodyPublishers.noBody() else HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString())
    }

    @Test fun `all management endpoints require admin and schedule CRUD round trips through JSON`() {
        for ((path, method) in listOf("schedules" to "GET", "groups" to "GET", "species" to "GET", "schedules" to "POST", "assignments" to "PUT", "groups/1" to "DELETE")) {
            assertEquals(401, request(path, method, null).statusCode(), "$method $path")
            assertEquals(403, request(path, method, "USER").statusCode(), "$method $path")
        }
        val list = request("schedules")
        assertEquals(200, list.statusCode())
        val original = mapper.treeToValue(mapper.readTree(list.body()).first(), SharedSchedule::class.java)
        val draft = original.copy(key = "api-test-${UUID.randomUUID()}", autoMatch = false, revision = 0)
        val created = request("schedules", "POST", body = draft)
        assertEquals(201, created.statusCode(), created.body())
        val saved = mapper.readValue(created.body(), SharedSchedule::class.java)
        assertEquals(1, saved.revision)
        assertEquals(draft.useSpeciesTiming, saved.useSpeciesTiming)
        assertEquals(200, request("schedules/${saved.key}").statusCode())
        val update = request("schedules/${saved.key}", "PUT", body = saved.copy(name = "Updated through API"))
        assertEquals(200, update.statusCode(), update.body())
        assertEquals(409, request("schedules/${saved.key}", "PUT", body = saved).statusCode())
        assertEquals(204, request("schedules/${saved.key}?revision=2", "DELETE").statusCode())
        assertEquals(404, request("schedules/${saved.key}").statusCode())
    }
}

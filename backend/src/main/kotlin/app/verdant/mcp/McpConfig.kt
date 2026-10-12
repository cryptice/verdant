package app.verdant.mcp

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

@ApplicationScoped
class McpConfig(
    @ConfigProperty(name = "verdant.mcp.public-base-url", defaultValue = "https://verdantplanner.com") baseUrl: String,
    @ConfigProperty(name = "verdant.mcp.frontend-base-url", defaultValue = "https://verdantplanner.com") frontendUrl: String,
) {
    val base = baseUrl.trimEnd('/')
    val frontend = frontendUrl.trimEnd('/')
    val resource = "$base/mcp"
    val metadata = "$base/.well-known/oauth-protected-resource/mcp"
    val challenge = "Bearer resource_metadata=\"$metadata\", scope=\"$READ_SCOPE\""
    fun acceptsOrigin(origin: String?) = origin == null || origin in setOf(base, frontend, "https://chatgpt.com", "https://chat.openai.com")
    fun acceptsResource(value: String?): Boolean = runCatching {
        val actual = URI(value ?: return false)
        val expected = URI(resource)
        actual.scheme.equals(expected.scheme, true) && actual.host.equals(expected.host, true) &&
            actual.port == expected.port && actual.rawPath == expected.rawPath && actual.rawQuery == null && actual.rawFragment == null && actual.rawUserInfo == null
    }.getOrDefault(false)
    companion object { const val READ_SCOPE = "verdant:read" }
}

object McpSecrets {
    private val random = SecureRandom()
    fun token(): String = ByteArray(32).also(random::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
    fun hash(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.US_ASCII)).joinToString("") { "%02x".format(it) }
    fun challenge(verifier: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
}

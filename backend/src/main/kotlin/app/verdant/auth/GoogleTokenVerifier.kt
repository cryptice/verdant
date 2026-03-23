package app.verdant.auth

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import java.math.BigInteger
import java.net.URI
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.RSAPublicKeySpec
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

@ApplicationScoped
class GoogleTokenVerifier(private val objectMapper: ObjectMapper) {

    private val log = Logger.getLogger(GoogleTokenVerifier::class.java.name)
    private val jwksUrl = "https://www.googleapis.com/oauth2/v3/certs"

    @Volatile
    private var cachedKeys: Map<String, JwkKey> = emptyMap()

    @Volatile
    private var cacheExpiry: Instant = Instant.EPOCH

    data class GoogleClaims(
        val sub: String,
        val email: String,
        val name: String,
        val picture: String?
    )

    private data class JwkKey(val n: String, val e: String)

    fun verify(idToken: String): GoogleClaims {
        val parts = idToken.split(".")
        if (parts.size != 3) throw IllegalArgumentException("Invalid token format")

        // Parse header to get key ID
        val header = objectMapper.readTree(String(Base64.getUrlDecoder().decode(parts[0])))
        val kid = header.get("kid")?.asText() ?: throw IllegalArgumentException("Missing kid in token header")
        val alg = header.get("alg")?.asText() ?: "RS256"
        if (alg != "RS256") throw IllegalArgumentException("Unsupported algorithm: $alg")

        // Verify signature using Google's public key
        val jwk = getKey(kid) ?: throw SecurityException("Unknown key ID: $kid")
        val nBytes = Base64.getUrlDecoder().decode(jwk.n)
        val eBytes = Base64.getUrlDecoder().decode(jwk.e)
        val pubKeySpec = RSAPublicKeySpec(BigInteger(1, nBytes), BigInteger(1, eBytes))
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(pubKeySpec)

        val signatureBytes = Base64.getUrlDecoder().decode(parts[2])
        val signedContent = "${parts[0]}.${parts[1]}".toByteArray(Charsets.UTF_8)
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initVerify(publicKey)
        sig.update(signedContent)
        if (!sig.verify(signatureBytes)) {
            throw SecurityException("Invalid token signature")
        }

        // Parse and validate payload
        val payload = String(Base64.getUrlDecoder().decode(parts[1]))
        val node = objectMapper.readTree(payload)

        // Check expiration
        val exp = node.get("exp")?.asLong() ?: throw IllegalArgumentException("Missing exp claim")
        if (Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
            throw SecurityException("Token has expired")
        }

        return GoogleClaims(
            sub = node.get("sub")?.asText() ?: throw IllegalArgumentException("Missing sub"),
            email = node.get("email")?.asText() ?: throw IllegalArgumentException("Missing email"),
            name = node.get("name")?.asText() ?: "User",
            picture = node.get("picture")?.asText()
        )
    }

    private fun getKey(kid: String): JwkKey? {
        if (Instant.now().isAfter(cacheExpiry)) {
            refreshKeys()
        }
        return cachedKeys[kid] ?: run {
            // Key not found; force refresh in case keys rotated
            refreshKeys()
            cachedKeys[kid]
        }
    }

    @Synchronized
    private fun refreshKeys() {
        try {
            val json = URI(jwksUrl).toURL().readText()
            val root = objectMapper.readTree(json)
            val keys = mutableMapOf<String, JwkKey>()
            for (key in root.get("keys")) {
                val kid = key.get("kid")?.asText() ?: continue
                val n = key.get("n")?.asText() ?: continue
                val e = key.get("e")?.asText() ?: continue
                keys[kid] = JwkKey(n = n, e = e)
            }
            cachedKeys = keys
            cacheExpiry = Instant.now().plusSeconds(600)
            log.fine("Refreshed ${keys.size} Google JWKS keys")
        } catch (ex: Exception) {
            log.warning("Failed to refresh Google JWKS keys: ${ex.message}")
            // Extend cache briefly to avoid hammering on transient failures
            if (cachedKeys.isNotEmpty()) {
                cacheExpiry = Instant.now().plusSeconds(60)
            } else {
                throw SecurityException("Unable to fetch Google public keys", ex)
            }
        }
    }
}

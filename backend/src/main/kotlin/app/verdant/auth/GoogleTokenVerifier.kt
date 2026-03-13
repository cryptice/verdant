package app.verdant.auth

import jakarta.enterprise.context.ApplicationScoped
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.Base64

@ApplicationScoped
class GoogleTokenVerifier(private val objectMapper: ObjectMapper) {

    data class GoogleClaims(
        val sub: String,
        val email: String,
        val name: String,
        val picture: String?
    )

    fun verify(idToken: String): GoogleClaims {
        // Decode the payload (second part of JWT)
        val parts = idToken.split(".")
        if (parts.size != 3) throw IllegalArgumentException("Invalid token format")

        val payload = String(Base64.getUrlDecoder().decode(parts[1]))
        val node = objectMapper.readTree(payload)

        return GoogleClaims(
            sub = node.get("sub")?.asText() ?: throw IllegalArgumentException("Missing sub"),
            email = node.get("email")?.asText() ?: throw IllegalArgumentException("Missing email"),
            name = node.get("name")?.asText() ?: "User",
            picture = node.get("picture")?.asText()
        )
    }
}

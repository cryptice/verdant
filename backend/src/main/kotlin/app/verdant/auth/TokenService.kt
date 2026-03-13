package app.verdant.auth

import app.verdant.entity.User
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import java.time.Duration

@ApplicationScoped
class TokenService {
    fun generateToken(user: User): String {
        return Jwt.issuer("verdant-api")
            .upn(user.email)
            .subject(user.id.toString())
            .groups(setOf(user.role.name))
            .claim("userId", user.id)
            .claim("displayName", user.displayName)
            .expiresIn(Duration.ofDays(30))
            .sign()
    }
}

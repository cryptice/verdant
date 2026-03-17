package app.verdant.auth

import app.verdant.entity.Role
import app.verdant.entity.User
import app.verdant.repository.UserRepository
import io.quarkus.elytron.security.common.BcryptUtil
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.Optional

@ApplicationScoped
class AdminSeeder(
    private val userRepository: UserRepository,
    @ConfigProperty(name = "verdant.admin.email", defaultValue = "verdant@l2c.se") private val adminEmail: String,
    @ConfigProperty(name = "verdant.admin.password") private val adminPassword: Optional<String>,
) {

    fun onStart(@Observes event: StartupEvent) {
        val password = adminPassword.orElse(null)
        if (password.isNullOrBlank()) return

        val existing = userRepository.findByEmail(adminEmail)
        if (existing == null) {
            userRepository.persist(
                User(
                    email = adminEmail,
                    displayName = "Admin",
                    role = Role.ADMIN,
                    passwordHash = BcryptUtil.bcryptHash(password),
                )
            )
        }
    }
}

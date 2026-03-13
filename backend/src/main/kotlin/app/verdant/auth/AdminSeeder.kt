package app.verdant.auth

import app.verdant.entity.Role
import app.verdant.entity.User
import app.verdant.repository.UserRepository
import io.quarkus.elytron.security.common.BcryptUtil
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes

@ApplicationScoped
class AdminSeeder(private val userRepository: UserRepository) {

    fun onStart(@Observes event: StartupEvent) {
        if (userRepository.findByEmail("admin@verdant.app") == null) {
            userRepository.persist(
                User(
                    email = "admin@verdant.app",
                    displayName = "Admin",
                    role = Role.ADMIN,
                    passwordHash = BcryptUtil.bcryptHash("admin"),
                )
            )
        }
    }
}

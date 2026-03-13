package app.verdant.repository

import app.verdant.entity.User
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class UserRepository : PanacheRepository<User> {
    fun findByGoogleSubject(subject: String): User? = find("googleSubject", subject).firstResult()
    fun findByEmail(email: String): User? = find("email", email).firstResult()
}

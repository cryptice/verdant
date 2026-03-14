package app.verdant.repository

import app.verdant.entity.Role
import app.verdant.entity.User
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement
import java.time.Instant

@ApplicationScoped
class UserRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): User? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM app_user WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toUser() else null }
            }
        }

    fun findByGoogleSubject(subject: String): User? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM app_user WHERE google_subject = ?").use { ps ->
                ps.setString(1, subject)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toUser() else null }
            }
        }

    fun findByEmail(email: String): User? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM app_user WHERE email = ?").use { ps ->
                ps.setString(1, email)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toUser() else null }
            }
        }

    fun listAll(): List<User> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM app_user ORDER BY id").use { ps ->
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toUser()) }
                }
            }
        }

    fun persist(user: User): User {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO app_user (google_subject, email, display_name, avatar_url, password_hash, role, language, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, now(), now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setString(1, user.googleSubject)
                ps.setString(2, user.email)
                ps.setString(3, user.displayName)
                ps.setString(4, user.avatarUrl)
                ps.setString(5, user.passwordHash)
                ps.setString(6, user.role.name)
                ps.setString(7, user.language)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return user.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(user: User) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE app_user SET google_subject = ?, email = ?, display_name = ?, avatar_url = ?,
                   password_hash = ?, role = ?, language = ?, updated_at = now() WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, user.googleSubject)
                ps.setString(2, user.email)
                ps.setString(3, user.displayName)
                ps.setString(4, user.avatarUrl)
                ps.setString(5, user.passwordHash)
                ps.setString(6, user.role.name)
                ps.setString(7, user.language)
                ps.setLong(8, user.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM app_user WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toUser() = User(
        id = getLong("id"),
        googleSubject = getString("google_subject"),
        email = getString("email"),
        displayName = getString("display_name"),
        avatarUrl = getString("avatar_url"),
        passwordHash = getString("password_hash"),
        role = Role.valueOf(getString("role")),
        language = getString("language") ?: "sv",
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}

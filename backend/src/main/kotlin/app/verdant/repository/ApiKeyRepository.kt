package app.verdant.repository

import app.verdant.entity.ApiKey
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class ApiKeyRepository(private val ds: AgroalDataSource) {

    fun create(userId: Long, orgId: Long, name: String, expiresAt: Instant): ApiKey =
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO api_key (id, user_id, org_id, name, expires_at) VALUES (?, ?, ?, ?, ?) RETURNING *"
            ).use { ps ->
                ps.setObject(1, UUID.randomUUID())
                ps.setLong(2, userId)
                ps.setLong(3, orgId)
                ps.setString(4, name)
                ps.setTimestamp(5, Timestamp.from(expiresAt))
                ps.executeQuery().use { rs -> rs.next(); rs.toApiKey() }
            }
        }

    fun findById(id: UUID): ApiKey? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM api_key WHERE id = ?").use { ps ->
                ps.setObject(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toApiKey() else null }
            }
        }

    /** The user's keys for the organization that are neither revoked nor expired, newest first. */
    fun findActive(userId: Long, orgId: Long): List<ApiKey> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT * FROM api_key WHERE user_id = ? AND org_id = ? AND revoked_at IS NULL AND expires_at > now()
                   ORDER BY created_at DESC"""
            ).use { ps ->
                ps.setLong(1, userId)
                ps.setLong(2, orgId)
                ps.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.toApiKey()) } }
            }
        }

    fun revoke(id: UUID) {
        ds.connection.use { conn ->
            conn.prepareStatement("UPDATE api_key SET revoked_at = COALESCE(revoked_at, now()) WHERE id = ?").use { ps ->
                ps.setObject(1, id)
                ps.executeUpdate()
            }
        }
    }

    /** Records use, at most once a minute per key so busy scripts don't write on every request. */
    fun touch(id: UUID) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "UPDATE api_key SET last_used_at = now() WHERE id = ? AND (last_used_at IS NULL OR last_used_at < now() - INTERVAL '1 minute')"
            ).use { ps ->
                ps.setObject(1, id)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toApiKey() = ApiKey(
        id = getObject("id", UUID::class.java),
        userId = getLong("user_id"),
        orgId = getLong("org_id"),
        name = getString("name"),
        createdAt = getTimestamp("created_at").toInstant(),
        expiresAt = getTimestamp("expires_at").toInstant(),
        lastUsedAt = getTimestamp("last_used_at")?.toInstant(),
        revokedAt = getTimestamp("revoked_at")?.toInstant(),
    )
}

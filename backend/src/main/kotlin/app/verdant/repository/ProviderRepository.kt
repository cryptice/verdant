package app.verdant.repository

import app.verdant.entity.Provider
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class ProviderRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): Provider? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM provider WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toProvider() else null }
            }
        }

    fun findAll(): List<Provider> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM provider ORDER BY name").use { ps ->
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toProvider()) }
                }
            }
        }

    fun persist(provider: Provider): Provider {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO provider (name, identifier) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setString(1, provider.name)
                ps.setString(2, provider.identifier)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return provider.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(provider: Provider) {
        ds.connection.use { conn ->
            conn.prepareStatement("UPDATE provider SET name = ?, identifier = ? WHERE id = ?").use { ps ->
                ps.setString(1, provider.name)
                ps.setString(2, provider.identifier)
                ps.setLong(3, provider.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM provider WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Provider not found")
            }
        }
    }

    private fun ResultSet.toProvider() = Provider(
        id = getLong("id"),
        name = getString("name"),
        identifier = getString("identifier"),
    )
}

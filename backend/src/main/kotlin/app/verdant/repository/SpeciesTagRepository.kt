package app.verdant.repository

import app.verdant.entity.SpeciesTag
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class SpeciesTagRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): SpeciesTag? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_tag WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toTag() else null }
            }
        }

    fun findByUserId(userId: Long): List<SpeciesTag> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_tag WHERE user_id = ? ORDER BY name").use { ps ->
                ps.setLong(1, userId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toTag()) }
                }
            }
        }

    fun persist(tag: SpeciesTag): SpeciesTag {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO species_tag (user_id, name) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, tag.userId)
                ps.setString(2, tag.name)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return tag.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM species_tag WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toTag() = SpeciesTag(
        id = getLong("id"),
        userId = getLong("user_id"),
        name = getString("name"),
    )
}

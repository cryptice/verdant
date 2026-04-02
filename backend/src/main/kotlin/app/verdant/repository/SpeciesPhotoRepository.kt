package app.verdant.repository

import app.verdant.entity.SpeciesPhoto
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class SpeciesPhotoRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): SpeciesPhoto? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_photo WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toPhoto() else null }
            }
        }

    fun findBySpeciesIds(speciesIds: Set<Long>): Map<Long, List<SpeciesPhoto>> {
        if (speciesIds.isEmpty()) return emptyMap()
        val placeholders = speciesIds.joinToString(",") { "?" }
        return ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_photo WHERE species_id IN ($placeholders) ORDER BY sort_order, id").use { ps ->
                speciesIds.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    val result = mutableMapOf<Long, MutableList<SpeciesPhoto>>()
                    while (rs.next()) {
                        val photo = rs.toPhoto()
                        result.getOrPut(photo.speciesId) { mutableListOf() }.add(photo)
                    }
                    result
                }
            }
        }
    }

    fun findBySpeciesId(speciesId: Long): List<SpeciesPhoto> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_photo WHERE species_id = ? ORDER BY sort_order, id").use { ps ->
                ps.setLong(1, speciesId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toPhoto()) }
                }
            }
        }

    fun persist(photo: SpeciesPhoto): SpeciesPhoto {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO species_photo (species_id, image_url, sort_order, created_at) VALUES (?, ?, ?, now())",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, photo.speciesId)
                ps.setString(2, photo.imageUrl)
                ps.setInt(3, photo.sortOrder)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return photo.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM species_photo WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Species photo not found")
            }
        }
    }

    fun deleteBySpeciesId(speciesId: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM species_photo WHERE species_id = ?").use { ps ->
                ps.setLong(1, speciesId)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toPhoto() = SpeciesPhoto(
        id = getLong("id"),
        speciesId = getLong("species_id"),
        imageUrl = getString("image_url"),
        sortOrder = getInt("sort_order"),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}

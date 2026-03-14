package app.verdant.repository

import app.verdant.entity.GrowingPosition
import app.verdant.entity.SoilType
import app.verdant.entity.Species
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class SpeciesRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): Species? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toSpecies() else null }
            }
        }

    fun findByUserId(userId: Long): List<Species> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species WHERE user_id = ? ORDER BY common_name").use { ps ->
                ps.setLong(1, userId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSpecies()) }
                }
            }
        }

    fun persist(species: Species): Species {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO species (user_id, common_name, scientific_name, image_base64,
                   days_to_sprout, days_to_harvest, germination_time_days, sowing_depth_mm,
                   growing_position, soil, height_cm, bloom_time, germination_rate, group_id, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, species.userId)
                ps.setString(2, species.commonName)
                ps.setString(3, species.scientificName)
                ps.setString(4, species.imageBase64)
                ps.setObject(5, species.daysToSprout)
                ps.setObject(6, species.daysToHarvest)
                ps.setObject(7, species.germinationTimeDays)
                ps.setObject(8, species.sowingDepthMm)
                ps.setString(9, species.growingPosition?.name)
                ps.setString(10, species.soil?.name)
                ps.setObject(11, species.heightCm)
                ps.setString(12, species.bloomTime)
                ps.setObject(13, species.germinationRate)
                ps.setObject(14, species.groupId)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return species.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(species: Species) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE species SET common_name = ?, scientific_name = ?, image_base64 = ?,
                   days_to_sprout = ?, days_to_harvest = ?, germination_time_days = ?,
                   sowing_depth_mm = ?, growing_position = ?, soil = ?, height_cm = ?,
                   bloom_time = ?, germination_rate = ?, group_id = ?
                   WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, species.commonName)
                ps.setString(2, species.scientificName)
                ps.setString(3, species.imageBase64)
                ps.setObject(4, species.daysToSprout)
                ps.setObject(5, species.daysToHarvest)
                ps.setObject(6, species.germinationTimeDays)
                ps.setObject(7, species.sowingDepthMm)
                ps.setString(8, species.growingPosition?.name)
                ps.setString(9, species.soil?.name)
                ps.setObject(10, species.heightCm)
                ps.setString(11, species.bloomTime)
                ps.setObject(12, species.germinationRate)
                ps.setObject(13, species.groupId)
                ps.setLong(14, species.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM species WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    fun findTagIdsForSpecies(speciesId: Long): List<Long> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT tag_id FROM species_tag_mapping WHERE species_id = ?").use { ps ->
                ps.setLong(1, speciesId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.getLong("tag_id")) }
                }
            }
        }

    fun setTagsForSpecies(speciesId: Long, tagIds: List<Long>) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM species_tag_mapping WHERE species_id = ?").use { ps ->
                ps.setLong(1, speciesId)
                ps.executeUpdate()
            }
            if (tagIds.isNotEmpty()) {
                conn.prepareStatement("INSERT INTO species_tag_mapping (species_id, tag_id) VALUES (?, ?)").use { ps ->
                    for (tagId in tagIds) {
                        ps.setLong(1, speciesId)
                        ps.setLong(2, tagId)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }
        }
    }

    private fun ResultSet.toSpecies() = Species(
        id = getLong("id"),
        userId = getLong("user_id"),
        commonName = getString("common_name"),
        scientificName = getString("scientific_name"),
        imageBase64 = getString("image_base64"),
        daysToSprout = getObject("days_to_sprout") as? Int,
        daysToHarvest = getObject("days_to_harvest") as? Int,
        germinationTimeDays = getObject("germination_time_days") as? Int,
        sowingDepthMm = getObject("sowing_depth_mm") as? Int,
        growingPosition = getString("growing_position")?.let { GrowingPosition.valueOf(it) },
        soil = getString("soil")?.let { SoilType.valueOf(it) },
        heightCm = getObject("height_cm") as? Int,
        bloomTime = getString("bloom_time"),
        germinationRate = getObject("germination_rate") as? Int,
        groupId = getObject("group_id") as? Long,
        createdAt = getTimestamp("created_at").toInstant(),
    )
}

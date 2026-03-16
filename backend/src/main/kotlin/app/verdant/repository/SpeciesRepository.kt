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
            conn.prepareStatement("SELECT * FROM species WHERE user_id = ? OR user_id IS NULL ORDER BY common_name").use { ps ->
                ps.setLong(1, userId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSpecies()) }
                }
            }
        }

    fun persist(species: Species): Species {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO species (user_id, common_name, common_name_sv, scientific_name, image_front_url, image_back_url,
                   days_to_sprout, days_to_harvest, germination_time_days, sowing_depth_mm,
                   growing_positions, soils, height_cm, bloom_time, germination_rate, group_id, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                if (species.userId != null) ps.setLong(1, species.userId) else ps.setNull(1, java.sql.Types.BIGINT)
                ps.setString(2, species.commonName)
                ps.setString(3, species.commonNameSv)
                ps.setString(4, species.scientificName)
                ps.setString(5, species.imageFrontUrl)
                ps.setString(6, species.imageBackUrl)
                ps.setObject(7, species.daysToSprout)
                ps.setObject(8, species.daysToHarvest)
                ps.setObject(9, species.germinationTimeDays)
                ps.setObject(10, species.sowingDepthMm)
                ps.setString(11, species.growingPositions.takeIf { it.isNotEmpty() }?.joinToString(",") { it.name })
                ps.setString(12, species.soils.takeIf { it.isNotEmpty() }?.joinToString(",") { it.name })
                ps.setObject(13, species.heightCm)
                ps.setString(14, species.bloomTime)
                ps.setObject(15, species.germinationRate)
                ps.setObject(16, species.groupId)
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
                """UPDATE species SET common_name = ?, common_name_sv = ?, scientific_name = ?,
                   image_front_url = ?, image_back_url = ?,
                   days_to_sprout = ?, days_to_harvest = ?, germination_time_days = ?,
                   sowing_depth_mm = ?, growing_positions = ?, soils = ?, height_cm = ?,
                   bloom_time = ?, germination_rate = ?, group_id = ?
                   WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, species.commonName)
                ps.setString(2, species.commonNameSv)
                ps.setString(3, species.scientificName)
                ps.setString(4, species.imageFrontUrl)
                ps.setString(5, species.imageBackUrl)
                ps.setObject(6, species.daysToSprout)
                ps.setObject(7, species.daysToHarvest)
                ps.setObject(8, species.germinationTimeDays)
                ps.setObject(9, species.sowingDepthMm)
                ps.setString(10, species.growingPositions.takeIf { it.isNotEmpty() }?.joinToString(",") { it.name })
                ps.setString(11, species.soils.takeIf { it.isNotEmpty() }?.joinToString(",") { it.name })
                ps.setObject(12, species.heightCm)
                ps.setString(13, species.bloomTime)
                ps.setObject(14, species.germinationRate)
                ps.setObject(15, species.groupId)
                ps.setLong(16, species.id!!)
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
        userId = getObject("user_id") as? Long,
        commonName = getString("common_name"),
        commonNameSv = getString("common_name_sv"),
        scientificName = getString("scientific_name"),
        imageFrontUrl = getString("image_front_url"),
        imageBackUrl = getString("image_back_url"),
        daysToSprout = getObject("days_to_sprout") as? Int,
        daysToHarvest = getObject("days_to_harvest") as? Int,
        germinationTimeDays = getObject("germination_time_days") as? Int,
        sowingDepthMm = getObject("sowing_depth_mm") as? Int,
        growingPositions = getString("growing_positions")?.split(",")?.map { GrowingPosition.valueOf(it) } ?: emptyList(),
        soils = getString("soils")?.split(",")?.map { SoilType.valueOf(it) } ?: emptyList(),
        heightCm = getObject("height_cm") as? Int,
        bloomTime = getString("bloom_time"),
        germinationRate = getObject("germination_rate") as? Int,
        groupId = getObject("group_id") as? Long,
        createdAt = getTimestamp("created_at").toInstant(),
    )
}

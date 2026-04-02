package app.verdant.repository

import app.verdant.entity.GrowingPosition
import app.verdant.entity.PlantType
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

    fun findAll(): List<Species> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species ORDER BY common_name").use { ps ->
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSpecies()) }
                }
            }
        }

    fun searchAll(query: String, limit: Int = 20): List<Species> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT * FROM species
                   WHERE common_name ILIKE ? OR common_name_sv ILIKE ? OR variant_name ILIKE ? OR variant_name_sv ILIKE ? OR scientific_name ILIKE ?
                   ORDER BY common_name_sv, common_name
                   LIMIT ?"""
            ).use { ps ->
                val pattern = "%$query%"
                ps.setString(1, pattern)
                ps.setString(2, pattern)
                ps.setString(3, pattern)
                ps.setString(4, pattern)
                ps.setString(5, pattern)
                ps.setInt(6, limit)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSpecies()) }
                }
            }
        }

    fun findByUserId(userId: Long, limit: Int = 50, offset: Int = 0): List<Species> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species WHERE user_id = ? OR user_id IS NULL ORDER BY common_name LIMIT ? OFFSET ?").use { ps ->
                ps.setLong(1, userId)
                ps.setInt(2, limit)
                ps.setInt(3, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSpecies()) }
                }
            }
        }

    fun searchByUserId(userId: Long, query: String, limit: Int = 20): List<Species> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT * FROM species
                   WHERE (user_id = ? OR user_id IS NULL)
                     AND (common_name ILIKE ? OR common_name_sv ILIKE ? OR variant_name ILIKE ? OR variant_name_sv ILIKE ? OR scientific_name ILIKE ?)
                   ORDER BY common_name_sv, common_name
                   LIMIT ?"""
            ).use { ps ->
                val pattern = "%$query%"
                ps.setLong(1, userId)
                ps.setString(2, pattern)
                ps.setString(3, pattern)
                ps.setString(4, pattern)
                ps.setString(5, pattern)
                ps.setString(6, pattern)
                ps.setInt(7, limit)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSpecies()) }
                }
            }
        }

    fun persist(species: Species): Species {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO species (user_id, common_name, variant_name, common_name_sv, variant_name_sv, scientific_name, image_front_url, image_back_url,
                   days_to_sprout, days_to_harvest, germination_time_days, sowing_depth_mm,
                   growing_positions, soils, height_cm, bloom_months, sowing_months, germination_rate, group_id,
                   cost_per_seed_sek, expected_stems_per_plant, expected_vase_life_days, plant_type, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                if (species.userId != null) ps.setLong(1, species.userId) else ps.setNull(1, java.sql.Types.BIGINT)
                ps.setString(2, species.commonName)
                ps.setString(3, species.variantName)
                ps.setString(4, species.commonNameSv)
                ps.setString(5, species.variantNameSv)
                ps.setString(6, species.scientificName)
                ps.setString(7, species.imageFrontUrl)
                ps.setString(8, species.imageBackUrl)
                ps.setObject(9, species.daysToSprout)
                ps.setObject(10, species.daysToHarvest)
                ps.setObject(11, species.germinationTimeDays)
                ps.setObject(12, species.sowingDepthMm)
                ps.setString(13, species.growingPositions.takeIf { it.isNotEmpty() }?.joinToString(",") { it.name })
                ps.setString(14, species.soils.takeIf { it.isNotEmpty() }?.joinToString(",") { it.name })
                ps.setObject(15, species.heightCm)
                ps.setString(16, species.bloomMonths.takeIf { it.isNotEmpty() }?.joinToString(","))
                ps.setString(17, species.sowingMonths.takeIf { it.isNotEmpty() }?.joinToString(","))
                ps.setObject(18, species.germinationRate)
                ps.setObject(19, species.groupId)
                ps.setObject(20, species.costPerSeedSek)
                ps.setObject(21, species.expectedStemsPerPlant)
                ps.setObject(22, species.expectedVaseLifeDays)
                ps.setString(23, species.plantType.name)
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
                """UPDATE species SET common_name = ?, variant_name = ?, common_name_sv = ?, variant_name_sv = ?, scientific_name = ?,
                   image_front_url = ?, image_back_url = ?,
                   days_to_sprout = ?, days_to_harvest = ?, germination_time_days = ?,
                   sowing_depth_mm = ?, growing_positions = ?, soils = ?, height_cm = ?,
                   bloom_months = ?, sowing_months = ?, germination_rate = ?, group_id = ?,
                   cost_per_seed_sek = ?, expected_stems_per_plant = ?, expected_vase_life_days = ?, plant_type = ?
                   WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, species.commonName)
                ps.setString(2, species.variantName)
                ps.setString(3, species.commonNameSv)
                ps.setString(4, species.variantNameSv)
                ps.setString(5, species.scientificName)
                ps.setString(6, species.imageFrontUrl)
                ps.setString(7, species.imageBackUrl)
                ps.setObject(8, species.daysToSprout)
                ps.setObject(9, species.daysToHarvest)
                ps.setObject(10, species.germinationTimeDays)
                ps.setObject(11, species.sowingDepthMm)
                ps.setString(12, species.growingPositions.takeIf { it.isNotEmpty() }?.joinToString(",") { it.name })
                ps.setString(13, species.soils.takeIf { it.isNotEmpty() }?.joinToString(",") { it.name })
                ps.setObject(14, species.heightCm)
                ps.setString(15, species.bloomMonths.takeIf { it.isNotEmpty() }?.joinToString(","))
                ps.setString(16, species.sowingMonths.takeIf { it.isNotEmpty() }?.joinToString(","))
                ps.setObject(17, species.germinationRate)
                ps.setObject(18, species.groupId)
                ps.setObject(19, species.costPerSeedSek)
                ps.setObject(20, species.expectedStemsPerPlant)
                ps.setObject(21, species.expectedVaseLifeDays)
                ps.setString(22, species.plantType.name)
                ps.setLong(23, species.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM species WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Species not found")
            }
        }
    }

    fun findByIds(ids: Set<Long>): Map<Long, Species> {
        if (ids.isEmpty()) return emptyMap()
        val placeholders = ids.joinToString(",") { "?" }
        return ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species WHERE id IN ($placeholders)").use { ps ->
                ids.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    buildMap { while (rs.next()) { val s = rs.toSpecies(); put(s.id!!, s) } }
                }
            }
        }
    }

    fun findNamesByIds(ids: Set<Long>): Map<Long, String> {
        if (ids.isEmpty()) return emptyMap()
        val placeholders = ids.joinToString(",") { "?" }
        return ds.connection.use { conn ->
            conn.prepareStatement("SELECT id, common_name FROM species WHERE id IN ($placeholders)").use { ps ->
                ids.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    buildMap { while (rs.next()) put(rs.getLong("id"), rs.getString("common_name")) }
                }
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

    fun findTagIdsBySpeciesIds(speciesIds: Set<Long>): Map<Long, List<Long>> {
        if (speciesIds.isEmpty()) return emptyMap()
        val placeholders = speciesIds.joinToString(",") { "?" }
        return ds.connection.use { conn ->
            conn.prepareStatement("SELECT species_id, tag_id FROM species_tag_mapping WHERE species_id IN ($placeholders)").use { ps ->
                speciesIds.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    val result = mutableMapOf<Long, MutableList<Long>>()
                    while (rs.next()) {
                        result.getOrPut(rs.getLong("species_id")) { mutableListOf() }.add(rs.getLong("tag_id"))
                    }
                    result
                }
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
        variantName = getString("variant_name"),
        commonNameSv = getString("common_name_sv"),
        variantNameSv = getString("variant_name_sv"),
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
        bloomMonths = getString("bloom_months")?.split(",")?.map { it.toInt() } ?: emptyList(),
        sowingMonths = getString("sowing_months")?.split(",")?.map { it.toInt() } ?: emptyList(),
        germinationRate = getObject("germination_rate") as? Int,
        groupId = getObject("group_id") as? Long,
        costPerSeedSek = getObject("cost_per_seed_sek") as? Int,
        expectedStemsPerPlant = getObject("expected_stems_per_plant") as? Int,
        expectedVaseLifeDays = getObject("expected_vase_life_days") as? Int,
        plantType = getString("plant_type")?.let { PlantType.valueOf(it) } ?: PlantType.ANNUAL,
        createdAt = getTimestamp("created_at").toInstant(),
    )
}

package app.verdant.repository

import app.verdant.entity.*
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet

@ApplicationScoped
class BedRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): Bed? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM bed WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toBed() else null }
            }
        }

    fun findByOrgIdWithGardenName(orgId: Long): List<BedWithGarden> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT b.*, g.name as garden_name FROM bed b
                   JOIN garden g ON b.garden_id = g.id
                   WHERE g.org_id = ? ORDER BY g.name, b.name"""
            ).use { ps ->
                ps.setLong(1, orgId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(
                            BedWithGarden(
                                bed = rs.toBed(),
                                gardenName = rs.getString("garden_name"),
                            )
                        )
                    }
                }
            }
        }

    fun countByGardenIds(gardenIds: Set<Long>): Map<Long, Int> {
        if (gardenIds.isEmpty()) return emptyMap()
        val placeholders = gardenIds.joinToString(",") { "?" }
        return ds.connection.use { conn ->
            conn.prepareStatement("SELECT garden_id, COUNT(*) FROM bed WHERE garden_id IN ($placeholders) GROUP BY garden_id").use { ps ->
                gardenIds.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    buildMap { while (rs.next()) put(rs.getLong("garden_id"), rs.getInt("count")) }
                }
            }
        }
    }

    fun findByGardenId(gardenId: Long): List<Bed> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM bed WHERE garden_id = ? ORDER BY id").use { ps ->
                ps.setLong(1, gardenId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toBed()) }
                }
            }
        }

    fun persist(bed: Bed): Bed {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO bed (name, description, garden_id, boundary_json, length_meters, width_meters,
                                    soil_type, soil_ph, sun_exposure, drainage, aspect, irrigation_type, protection, raised_bed,
                                    created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())""",
                arrayOf("id")
            ).use { ps ->
                ps.setString(1, bed.name)
                ps.setString(2, bed.description)
                ps.setLong(3, bed.gardenId)
                ps.setString(4, bed.boundaryJson)
                bed.lengthMeters?.let { ps.setDouble(5, it) } ?: ps.setNull(5, java.sql.Types.DOUBLE)
                bed.widthMeters?.let { ps.setDouble(6, it) } ?: ps.setNull(6, java.sql.Types.DOUBLE)
                ps.setString(7, bed.soilType?.name)
                bed.soilPh?.let { ps.setDouble(8, it) } ?: ps.setNull(8, java.sql.Types.DOUBLE)
                ps.setString(9, bed.sunExposure?.name)
                ps.setString(10, bed.drainage?.name)
                ps.setString(11, bed.aspect?.name)
                ps.setString(12, bed.irrigationType?.name)
                ps.setString(13, bed.protection?.name)
                bed.raisedBed?.let { ps.setBoolean(14, it) } ?: ps.setNull(14, java.sql.Types.BOOLEAN)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return bed.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(bed: Bed) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE bed SET name = ?, description = ?, boundary_json = ?,
                                  length_meters = ?, width_meters = ?,
                                  soil_type = ?, soil_ph = ?, sun_exposure = ?, drainage = ?, aspect = ?,
                                  irrigation_type = ?, protection = ?, raised_bed = ?,
                                  updated_at = now()
                   WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, bed.name)
                ps.setString(2, bed.description)
                ps.setString(3, bed.boundaryJson)
                bed.lengthMeters?.let { ps.setDouble(4, it) } ?: ps.setNull(4, java.sql.Types.DOUBLE)
                bed.widthMeters?.let { ps.setDouble(5, it) } ?: ps.setNull(5, java.sql.Types.DOUBLE)
                ps.setString(6, bed.soilType?.name)
                bed.soilPh?.let { ps.setDouble(7, it) } ?: ps.setNull(7, java.sql.Types.DOUBLE)
                ps.setString(8, bed.sunExposure?.name)
                ps.setString(9, bed.drainage?.name)
                ps.setString(10, bed.aspect?.name)
                ps.setString(11, bed.irrigationType?.name)
                ps.setString(12, bed.protection?.name)
                bed.raisedBed?.let { ps.setBoolean(13, it) } ?: ps.setNull(13, java.sql.Types.BOOLEAN)
                ps.setLong(14, bed.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM bed WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Bed not found")
            }
        }
    }

    private fun ResultSet.toBed(): Bed = Bed(
        id = getLong("id"),
        name = getString("name"),
        description = getString("description"),
        gardenId = getLong("garden_id"),
        boundaryJson = getString("boundary_json"),
        lengthMeters = getDouble("length_meters").takeIf { !wasNull() },
        widthMeters = getDouble("width_meters").takeIf { !wasNull() },
        soilType = getString("soil_type")?.let { SoilType.valueOf(it) },
        soilPh = getDouble("soil_ph").takeIf { !wasNull() },
        sunExposure = getString("sun_exposure")?.let { SunExposure.valueOf(it) },
        drainage = getString("drainage")?.let { Drainage.valueOf(it) },
        aspect = getString("aspect")?.let { Aspect.valueOf(it) },
        irrigationType = getString("irrigation_type")?.let { IrrigationType.valueOf(it) },
        protection = getString("protection")?.let { Protection.valueOf(it) },
        raisedBed = getBoolean("raised_bed").takeIf { !wasNull() },
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}

data class BedWithGarden(
    val bed: Bed,
    val gardenName: String,
)

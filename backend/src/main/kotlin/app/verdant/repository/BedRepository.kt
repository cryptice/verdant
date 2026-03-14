package app.verdant.repository

import app.verdant.entity.Bed
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class BedRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): Bed? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM bed WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toBed() else null }
            }
        }

    fun findByUserIdWithGardenName(userId: Long): List<BedWithGarden> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT b.*, g.name as garden_name FROM bed b
                   JOIN garden g ON b.garden_id = g.id
                   WHERE g.owner_id = ? ORDER BY g.name, b.name"""
            ).use { ps ->
                ps.setLong(1, userId)
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
                """INSERT INTO bed (name, description, garden_id, boundary_json, created_at, updated_at)
                   VALUES (?, ?, ?, ?, now(), now())""",
                arrayOf("id")
            ).use { ps ->
                ps.setString(1, bed.name)
                ps.setString(2, bed.description)
                ps.setLong(3, bed.gardenId)
                ps.setString(4, bed.boundaryJson)
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
                "UPDATE bed SET name = ?, description = ?, boundary_json = ?, updated_at = now() WHERE id = ?"
            ).use { ps ->
                ps.setString(1, bed.name)
                ps.setString(2, bed.description)
                ps.setString(3, bed.boundaryJson)
                ps.setLong(4, bed.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM bed WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toBed(): Bed = Bed(
        id = getLong("id"),
        name = getString("name"),
        description = getString("description"),
        gardenId = getLong("garden_id"),
        boundaryJson = getString("boundary_json"),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}

data class BedWithGarden(
    val bed: Bed,
    val gardenName: String,
)

package app.verdant.repository

import app.verdant.entity.BedPhotoReason
import app.verdant.entity.GardenAreaPhoto
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp

@ApplicationScoped
class GardenAreaPhotoRepository(private val ds: AgroalDataSource) {

    fun persist(photo: GardenAreaPhoto): GardenAreaPhoto = ds.connection.use { conn ->
        conn.prepareStatement(
            """INSERT INTO garden_area_photo (garden_area_id, photo_url, reason, description, captured_at, created_at)
               VALUES (?, ?, ?, ?, ?, now())""",
            Statement.RETURN_GENERATED_KEYS,
        ).use { ps ->
            ps.setLong(1, photo.gardenAreaId)
            ps.setString(2, photo.photoUrl)
            ps.setString(3, photo.reason.name)
            ps.setString(4, photo.description)
            ps.setTimestamp(5, Timestamp.from(photo.capturedAt))
            ps.executeUpdate()
            ps.generatedKeys.use { rs -> rs.next(); photo.copy(id = rs.getLong(1)) }
        }
    }

    fun findById(id: Long): GardenAreaPhoto? = ds.connection.use { conn ->
        conn.prepareStatement("SELECT * FROM garden_area_photo WHERE id = ?").use { ps ->
            ps.setLong(1, id)
            ps.executeQuery().use { rs -> if (rs.next()) rs.toPhoto() else null }
        }
    }

    fun findByAreaId(areaId: Long): List<GardenAreaPhoto> = ds.connection.use { conn ->
        conn.prepareStatement(
            """SELECT * FROM garden_area_photo WHERE garden_area_id = ?
               ORDER BY captured_at DESC, id DESC"""
        ).use { ps ->
            ps.setLong(1, areaId)
            ps.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.toPhoto()) } }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM garden_area_photo WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toPhoto() = GardenAreaPhoto(
        id = getLong("id"),
        gardenAreaId = getLong("garden_area_id"),
        photoUrl = getString("photo_url"),
        reason = BedPhotoReason.valueOf(getString("reason")),
        description = getString("description"),
        capturedAt = getTimestamp("captured_at").toInstant(),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}

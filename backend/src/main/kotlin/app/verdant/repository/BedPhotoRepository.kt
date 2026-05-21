package app.verdant.repository

import app.verdant.entity.BedPhoto
import app.verdant.entity.BedPhotoReason
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp

@ApplicationScoped
class BedPhotoRepository(private val ds: AgroalDataSource) {

    fun persist(photo: BedPhoto): BedPhoto = ds.connection.use { conn ->
        conn.prepareStatement(
            """INSERT INTO bed_photo (bed_id, photo_url, reason, description, captured_at, created_at)
               VALUES (?, ?, ?, ?, ?, now())""",
            Statement.RETURN_GENERATED_KEYS,
        ).use { ps ->
            ps.setLong(1, photo.bedId)
            ps.setString(2, photo.photoUrl)
            ps.setString(3, photo.reason.name)
            ps.setString(4, photo.description)
            ps.setTimestamp(5, Timestamp.from(photo.capturedAt))
            ps.executeUpdate()
            ps.generatedKeys.use { rs -> rs.next(); photo.copy(id = rs.getLong(1)) }
        }
    }

    fun findById(id: Long): BedPhoto? = ds.connection.use { conn ->
        conn.prepareStatement("SELECT * FROM bed_photo WHERE id = ?").use { ps ->
            ps.setLong(1, id)
            ps.executeQuery().use { rs -> if (rs.next()) rs.toBedPhoto() else null }
        }
    }

    fun findByBedId(bedId: Long): List<BedPhoto> = ds.connection.use { conn ->
        conn.prepareStatement(
            """SELECT * FROM bed_photo WHERE bed_id = ? ORDER BY captured_at DESC, id DESC"""
        ).use { ps ->
            ps.setLong(1, bedId)
            ps.executeQuery().use { rs ->
                buildList { while (rs.next()) add(rs.toBedPhoto()) }
            }
        }
    }

    fun updatePhotoUrl(id: Long, url: String) {
        ds.connection.use { conn ->
            conn.prepareStatement("UPDATE bed_photo SET photo_url = ? WHERE id = ?").use { ps ->
                ps.setString(1, url)
                ps.setLong(2, id)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long): Boolean = ds.connection.use { conn ->
        conn.prepareStatement("DELETE FROM bed_photo WHERE id = ?").use { ps ->
            ps.setLong(1, id)
            ps.executeUpdate() > 0
        }
    }

    private fun ResultSet.toBedPhoto() = BedPhoto(
        id = getLong("id"),
        bedId = getLong("bed_id"),
        photoUrl = getString("photo_url"),
        reason = BedPhotoReason.valueOf(getString("reason")),
        description = getString("description"),
        capturedAt = getTimestamp("captured_at").toInstant(),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}

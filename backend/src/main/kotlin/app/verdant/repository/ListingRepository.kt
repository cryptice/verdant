package app.verdant.repository

import app.verdant.entity.Listing
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class ListingRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): Listing? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM listing WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toListing() else null }
            }
        }

    fun findByIdForUpdate(id: Long): Listing? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM listing WHERE id = ? FOR UPDATE").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toListing() else null }
            }
        }

    fun findByUserId(userId: Long, limit: Int = 50, offset: Int = 0): List<Listing> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM listing WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?").use { ps ->
                ps.setLong(1, userId)
                ps.setInt(2, limit)
                ps.setInt(3, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toListing()) }
                }
            }
        }

    fun findActive(limit: Int = 50, offset: Int = 0): List<Listing> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT * FROM listing
                   WHERE is_active = true AND available_from <= CURRENT_DATE AND available_until >= CURRENT_DATE
                   ORDER BY created_at DESC LIMIT ? OFFSET ?"""
            ).use { ps ->
                ps.setInt(1, limit)
                ps.setInt(2, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toListing()) }
                }
            }
        }

    fun persist(listing: Listing): Listing {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO listing (user_id, species_id, title, description, quantity_available,
                   price_per_stem_cents, available_from, available_until, image_url, is_active, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, listing.userId)
                ps.setLong(2, listing.speciesId)
                ps.setString(3, listing.title)
                ps.setString(4, listing.description)
                ps.setInt(5, listing.quantityAvailable)
                ps.setInt(6, listing.pricePerStemCents)
                ps.setObject(7, listing.availableFrom)
                ps.setObject(8, listing.availableUntil)
                ps.setString(9, listing.imageUrl)
                ps.setBoolean(10, listing.isActive)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return listing.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(listing: Listing) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE listing SET title = ?, description = ?, quantity_available = ?,
                   price_per_stem_cents = ?, available_from = ?, available_until = ?,
                   image_url = ?, is_active = ?, updated_at = now()
                   WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, listing.title)
                ps.setString(2, listing.description)
                ps.setInt(3, listing.quantityAvailable)
                ps.setInt(4, listing.pricePerStemCents)
                ps.setObject(5, listing.availableFrom)
                ps.setObject(6, listing.availableUntil)
                ps.setString(7, listing.imageUrl)
                ps.setBoolean(8, listing.isActive)
                ps.setLong(9, listing.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM listing WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Listing not found")
            }
        }
    }

    private fun ResultSet.toListing() = Listing(
        id = getLong("id"),
        userId = getLong("user_id"),
        speciesId = getLong("species_id"),
        title = getString("title"),
        description = getString("description"),
        quantityAvailable = getInt("quantity_available"),
        pricePerStemCents = getInt("price_per_stem_cents"),
        availableFrom = getObject("available_from", java.time.LocalDate::class.java),
        availableUntil = getObject("available_until", java.time.LocalDate::class.java),
        imageUrl = getString("image_url"),
        isActive = getBoolean("is_active"),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}

package app.verdant.repository

import app.verdant.entity.SpeciesProvider
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class SpeciesProviderRepository(private val ds: AgroalDataSource) {

    fun findBySpeciesId(speciesId: Long): List<SpeciesProvider> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_provider WHERE species_id = ? ORDER BY id").use { ps ->
                ps.setLong(1, speciesId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSpeciesProvider()) }
                }
            }
        }

    fun findById(id: Long): SpeciesProvider? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_provider WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toSpeciesProvider() else null }
            }
        }

    fun persist(sp: SpeciesProvider): SpeciesProvider {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO species_provider (species_id, provider_id, image_front_url, image_back_url, product_url, created_at)
                   VALUES (?, ?, ?, ?, ?, now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, sp.speciesId)
                ps.setLong(2, sp.providerId)
                ps.setString(3, sp.imageFrontUrl)
                ps.setString(4, sp.imageBackUrl)
                ps.setString(5, sp.productUrl)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return sp.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(sp: SpeciesProvider) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE species_provider SET image_front_url = ?, image_back_url = ?, product_url = ?
                   WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, sp.imageFrontUrl)
                ps.setString(2, sp.imageBackUrl)
                ps.setString(3, sp.productUrl)
                ps.setLong(4, sp.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM species_provider WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    fun deleteBySpeciesId(speciesId: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM species_provider WHERE species_id = ?").use { ps ->
                ps.setLong(1, speciesId)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toSpeciesProvider() = SpeciesProvider(
        id = getLong("id"),
        speciesId = getLong("species_id"),
        providerId = getLong("provider_id"),
        imageFrontUrl = getString("image_front_url"),
        imageBackUrl = getString("image_back_url"),
        productUrl = getString("product_url"),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}

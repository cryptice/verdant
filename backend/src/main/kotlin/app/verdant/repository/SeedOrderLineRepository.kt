package app.verdant.repository

import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.Connection

@ApplicationScoped
class SeedOrderLineRepository(private val ds: AgroalDataSource) {

    /** Saved order quantities for the season, by species. */
    fun findBySeason(orgId: Long, seasonId: Long): Map<Long, Int> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT species_id, quantity FROM seed_order_line WHERE org_id = ? AND season_id = ?"
            ).use { ps ->
                ps.setLong(1, orgId)
                ps.setLong(2, seasonId)
                ps.executeQuery().use { rs ->
                    buildMap { while (rs.next()) put(rs.getLong(1), rs.getInt(2)) }
                }
            }
        }

    /** Replaces the season's order list with [quantities] (species id to quantity). */
    fun replaceForSeason(orgId: Long, seasonId: Long, quantities: Map<Long, Int>) = transaction { conn ->
        conn.prepareStatement("DELETE FROM seed_order_line WHERE org_id = ? AND season_id = ?").use { ps ->
            ps.setLong(1, orgId)
            ps.setLong(2, seasonId)
            ps.executeUpdate()
        }
        conn.prepareStatement(
            "INSERT INTO seed_order_line (org_id, season_id, species_id, quantity, updated_at) VALUES (?, ?, ?, ?, now())"
        ).use { ps ->
            quantities.forEach { (speciesId, quantity) ->
                ps.setLong(1, orgId)
                ps.setLong(2, seasonId)
                ps.setLong(3, speciesId)
                ps.setInt(4, quantity)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun <T> transaction(block: (Connection) -> T): T = ds.connection.use { conn ->
        conn.autoCommit = false
        try { val result = block(conn); conn.commit(); result } catch (e: Exception) { conn.rollback(); throw e }
        finally { conn.autoCommit = true }
    }
}

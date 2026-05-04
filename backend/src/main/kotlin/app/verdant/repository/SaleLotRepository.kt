package app.verdant.repository

import app.verdant.entity.SaleLot
import app.verdant.entity.SaleLotStatus
import app.verdant.entity.SourceKind
import app.verdant.entity.UnitKind
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Types

@ApplicationScoped
class SaleLotRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): SaleLot? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM sale_lot WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toSaleLot() else null }
            }
        }

    /**
     * Filtered list. [status] and [sourceKind] are optional; null means no filter.
     */
    fun findByOrgId(
        orgId: Long,
        status: SaleLotStatus? = null,
        sourceKind: SourceKind? = null,
        limit: Int = 200,
        offset: Int = 0,
    ): List<SaleLot> {
        val sql = buildString {
            append("SELECT * FROM sale_lot WHERE org_id = ?")
            if (status != null) append(" AND status = ?")
            if (sourceKind != null) append(" AND source_kind = ?")
            append(" ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?")
        }
        return ds.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                var i = 1
                ps.setLong(i++, orgId)
                if (status != null) ps.setString(i++, status.name)
                if (sourceKind != null) ps.setString(i++, sourceKind.name)
                ps.setInt(i++, limit)
                ps.setInt(i++, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSaleLot()) }
                }
            }
        }
    }

    fun findByPlantId(plantId: Long): List<SaleLot> = findBySourceColumn("plant_id", plantId)
    fun findByHarvestEventId(harvestEventId: Long): List<SaleLot> = findBySourceColumn("harvest_event_id", harvestEventId)
    fun findByBouquetId(bouquetId: Long): List<SaleLot> = findBySourceColumn("bouquet_id", bouquetId)

    private fun findBySourceColumn(column: String, value: Long): List<SaleLot> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM sale_lot WHERE $column = ? ORDER BY id").use { ps ->
                ps.setLong(1, value)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSaleLot()) }
                }
            }
        }

    fun persist(lot: SaleLot): SaleLot {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO sale_lot (
                       org_id, source_kind, plant_id, harvest_event_id, bouquet_id,
                       unit_kind, stems_per_unit,
                       quantity_total, quantity_remaining,
                       initial_requested_price_cents, current_requested_price_cents,
                       current_outlet_id, status, created_at, updated_at
                   ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())""",
                Statement.RETURN_GENERATED_KEYS,
            ).use { ps ->
                ps.setLong(1, lot.orgId)
                ps.setString(2, lot.sourceKind.name)
                ps.setObject(3, lot.plantId)
                ps.setObject(4, lot.harvestEventId)
                ps.setObject(5, lot.bouquetId)
                ps.setString(6, lot.unitKind.name)
                lot.stemsPerUnit?.let { ps.setInt(7, it) } ?: ps.setNull(7, Types.INTEGER)
                ps.setInt(8, lot.quantityTotal)
                ps.setInt(9, lot.quantityRemaining)
                ps.setInt(10, lot.initialRequestedPriceCents)
                ps.setInt(11, lot.currentRequestedPriceCents)
                ps.setLong(12, lot.currentOutletId)
                ps.setString(13, lot.status.name)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return lot.copy(id = rs.getLong(1))
                }
            }
        }
    }

    /**
     * Updates the mutable fields. Source FKs and source_kind / unit_kind / stems_per_unit
     * / quantity_total / initial_requested_price_cents are immutable post-creation —
     * only price (current), outlet (current), quantity_remaining, and status change.
     */
    fun update(lot: SaleLot) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE sale_lot
                   SET quantity_remaining = ?,
                       current_requested_price_cents = ?,
                       current_outlet_id = ?,
                       status = ?,
                       updated_at = now()
                   WHERE id = ?""",
            ).use { ps ->
                ps.setInt(1, lot.quantityRemaining)
                ps.setInt(2, lot.currentRequestedPriceCents)
                ps.setLong(3, lot.currentOutletId)
                ps.setString(4, lot.status.name)
                ps.setLong(5, lot.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM sale_lot WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Sale lot not found")
            }
        }
    }

    /**
     * Plant batch: available = COALESCE(surviving_count, seed_count, 0) − Σ(quantity_total)
     * over non-NOT_SOLD lots from this plant.
     *
     * Sales do NOT auto-decrement plant counts; the lot's quantity_total represents
     * the user's commitment of inventory. Returning lots to NOT_SOLD frees their
     * commitment back to available.
     */
    fun availableForPlant(plantId: Long): Int =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT COALESCE(p.surviving_count, p.seed_count, 0) -
                          COALESCE((SELECT SUM(quantity_total) FROM sale_lot
                                    WHERE plant_id = p.id AND status <> 'NOT_SOLD'), 0) AS available
                   FROM plant p WHERE p.id = ?""",
            ).use { ps ->
                ps.setLong(1, plantId)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getInt("available") else 0 }
            }
        }

    /**
     * Harvest event: available = stem_count − Σ(quantity_total × COALESCE(stems_per_unit, 1))
     * over non-NOT_SOLD lots from this harvest. The multiplier collapses BUNCH lots back
     * to stems; STEM lots have stems_per_unit = NULL → 1.
     */
    fun availableForHarvestEvent(harvestEventId: Long): Int =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT COALESCE(pe.stem_count, 0) -
                          COALESCE((SELECT SUM(quantity_total * COALESCE(stems_per_unit, 1))
                                    FROM sale_lot
                                    WHERE harvest_event_id = pe.id AND status <> 'NOT_SOLD'), 0) AS available
                   FROM plant_event pe WHERE pe.id = ?""",
            ).use { ps ->
                ps.setLong(1, harvestEventId)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getInt("available") else 0 }
            }
        }

    /**
     * Bouquet: available is 0 or 1 — a bouquet sells at most once. Returns 1 if no
     * non-NOT_SOLD lot exists for this bouquet, 0 otherwise.
     */
    fun availableForBouquet(bouquetId: Long): Int =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT 1 - COALESCE((SELECT 1 FROM sale_lot
                                        WHERE bouquet_id = ? AND status <> 'NOT_SOLD'
                                        LIMIT 1), 0) AS available""",
            ).use { ps ->
                ps.setLong(1, bouquetId)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getInt("available") else 1 }
            }
        }

    private fun ResultSet.toSaleLot() = SaleLot(
        id = getLong("id"),
        orgId = getLong("org_id"),
        sourceKind = SourceKind.valueOf(getString("source_kind")),
        plantId = getLong("plant_id").takeIf { !wasNull() },
        harvestEventId = getLong("harvest_event_id").takeIf { !wasNull() },
        bouquetId = getLong("bouquet_id").takeIf { !wasNull() },
        unitKind = UnitKind.valueOf(getString("unit_kind")),
        stemsPerUnit = getInt("stems_per_unit").takeIf { !wasNull() },
        quantityTotal = getInt("quantity_total"),
        quantityRemaining = getInt("quantity_remaining"),
        initialRequestedPriceCents = getInt("initial_requested_price_cents"),
        currentRequestedPriceCents = getInt("current_requested_price_cents"),
        currentOutletId = getLong("current_outlet_id"),
        status = SaleLotStatus.valueOf(getString("status")),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}

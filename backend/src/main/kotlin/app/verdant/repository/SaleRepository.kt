package app.verdant.repository

import app.verdant.entity.Sale
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.Date
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Types

@ApplicationScoped
class SaleRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): Sale? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM sale WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toSale() else null }
            }
        }

    fun findByLotId(lotId: Long): List<Sale> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT * FROM sale WHERE sale_lot_id = ? ORDER BY sold_at DESC, id DESC",
            ).use { ps ->
                ps.setLong(1, lotId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSale()) }
                }
            }
        }

    /**
     * List sales for an org, optionally bounded by sold_at date range. Joins
     * sale_lot for source kind, outlet for name, customer (optional) for name.
     * The source summary itself is filled in by the service layer using the
     * existing per-source-kind enrichment so we don't duplicate the
     * polymorphic-join logic here.
     */
    fun listForOrg(
        orgId: Long,
        fromDate: java.time.LocalDate?,
        toDate: java.time.LocalDate?,
        limit: Int,
        offset: Int,
    ): List<SaleListRow> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """
                SELECT s.id, s.sale_lot_id, s.quantity, s.price_per_unit_cents, s.sold_at,
                       sl.source_kind, sl.unit_kind, sl.plant_id, sl.harvest_event_id, sl.bouquet_id,
                       o.name AS outlet_name,
                       c.name AS customer_name
                FROM sale s
                JOIN sale_lot sl ON sl.id = s.sale_lot_id
                JOIN outlet o ON o.id = s.outlet_id
                LEFT JOIN customer c ON c.id = s.customer_id
                WHERE sl.org_id = ?
                  AND (CAST(? AS DATE) IS NULL OR s.sold_at >= ?)
                  AND (CAST(? AS DATE) IS NULL OR s.sold_at <= ?)
                ORDER BY s.sold_at DESC, s.created_at DESC, s.id DESC
                LIMIT ? OFFSET ?
                """.trimIndent(),
            ).use { ps ->
                ps.setLong(1, orgId)
                if (fromDate != null) {
                    ps.setDate(2, java.sql.Date.valueOf(fromDate))
                    ps.setDate(3, java.sql.Date.valueOf(fromDate))
                } else {
                    ps.setNull(2, java.sql.Types.DATE)
                    ps.setNull(3, java.sql.Types.DATE)
                }
                if (toDate != null) {
                    ps.setDate(4, java.sql.Date.valueOf(toDate))
                    ps.setDate(5, java.sql.Date.valueOf(toDate))
                } else {
                    ps.setNull(4, java.sql.Types.DATE)
                    ps.setNull(5, java.sql.Types.DATE)
                }
                ps.setInt(6, limit)
                ps.setInt(7, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toListRow()) }
                }
            }
        }

    private fun ResultSet.toListRow() = SaleListRow(
        id = getLong("id"),
        saleLotId = getLong("sale_lot_id"),
        quantity = getInt("quantity"),
        pricePerUnitCents = getInt("price_per_unit_cents"),
        soldAt = getDate("sold_at").toLocalDate(),
        sourceKind = getString("source_kind"),
        unitKind = getString("unit_kind"),
        plantId = getLong("plant_id").takeIf { !wasNull() },
        harvestEventId = getLong("harvest_event_id").takeIf { !wasNull() },
        bouquetId = getLong("bouquet_id").takeIf { !wasNull() },
        outletName = getString("outlet_name"),
        customerName = getString("customer_name"),
    )

    /** Intermediate row from the listing query; service enriches to SaleLedgerEntry. */
    data class SaleListRow(
        val id: Long,
        val saleLotId: Long,
        val quantity: Int,
        val pricePerUnitCents: Int,
        val soldAt: java.time.LocalDate,
        val sourceKind: String,
        val unitKind: String,
        val plantId: Long?,
        val harvestEventId: Long?,
        val bouquetId: Long?,
        val outletName: String,
        val customerName: String?,
    )

    /** Sum of quantity for a lot — used by service to recompute quantity_remaining. */
    fun sumQuantityForLot(lotId: Long): Int =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT COALESCE(SUM(quantity), 0) AS total FROM sale WHERE sale_lot_id = ?").use { ps ->
                ps.setLong(1, lotId)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getInt("total") else 0 }
            }
        }

    fun persist(sale: Sale): Sale {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO sale (
                       sale_lot_id, quantity, price_per_unit_cents, outlet_id, customer_id,
                       sold_at, recorded_by_user_id, notes, created_at
                   ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, now())""",
                Statement.RETURN_GENERATED_KEYS,
            ).use { ps ->
                ps.setLong(1, sale.saleLotId)
                ps.setInt(2, sale.quantity)
                ps.setInt(3, sale.pricePerUnitCents)
                ps.setLong(4, sale.outletId)
                sale.customerId?.let { ps.setLong(5, it) } ?: ps.setNull(5, Types.BIGINT)
                ps.setDate(6, Date.valueOf(sale.soldAt))
                ps.setLong(7, sale.recordedByUserId)
                ps.setString(8, sale.notes)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return sale.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(sale: Sale) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE sale SET quantity = ?, price_per_unit_cents = ?, customer_id = ?,
                                   sold_at = ?, notes = ?
                   WHERE id = ?""",
            ).use { ps ->
                ps.setInt(1, sale.quantity)
                ps.setInt(2, sale.pricePerUnitCents)
                sale.customerId?.let { ps.setLong(3, it) } ?: ps.setNull(3, Types.BIGINT)
                ps.setDate(4, Date.valueOf(sale.soldAt))
                ps.setString(5, sale.notes)
                ps.setLong(6, sale.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM sale WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Sale not found")
            }
        }
    }

    private fun ResultSet.toSale() = Sale(
        id = getLong("id"),
        saleLotId = getLong("sale_lot_id"),
        quantity = getInt("quantity"),
        pricePerUnitCents = getInt("price_per_unit_cents"),
        outletId = getLong("outlet_id"),
        customerId = getLong("customer_id").takeIf { !wasNull() },
        soldAt = getDate("sold_at").toLocalDate(),
        recordedByUserId = getLong("recorded_by_user_id"),
        notes = getString("notes"),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}

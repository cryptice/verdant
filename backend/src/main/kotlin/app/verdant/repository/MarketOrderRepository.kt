package app.verdant.repository

import app.verdant.entity.MarketOrder
import app.verdant.entity.OrderStatus
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class MarketOrderRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): MarketOrder? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM market_order WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toMarketOrder() else null }
            }
        }

    fun findByPurchaserId(userId: Long): List<MarketOrder> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM market_order WHERE purchaser_id = ? ORDER BY created_at DESC").use { ps ->
                ps.setLong(1, userId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toMarketOrder()) }
                }
            }
        }

    fun findByProducerId(userId: Long): List<MarketOrder> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM market_order WHERE producer_id = ? ORDER BY created_at DESC").use { ps ->
                ps.setLong(1, userId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toMarketOrder()) }
                }
            }
        }

    fun persist(order: MarketOrder): MarketOrder {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO market_order (purchaser_id, producer_id, status, delivery_date, total_cents, notes, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, now(), now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, order.purchaserId)
                ps.setLong(2, order.producerId)
                ps.setString(3, order.status.name)
                ps.setObject(4, order.deliveryDate)
                ps.setInt(5, order.totalCents)
                ps.setString(6, order.notes)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return order.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun updateStatus(id: Long, status: OrderStatus) {
        ds.connection.use { conn ->
            conn.prepareStatement("UPDATE market_order SET status = ?, updated_at = now() WHERE id = ?").use { ps ->
                ps.setString(1, status.name)
                ps.setLong(2, id)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM market_order WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Order not found")
            }
        }
    }

    private fun ResultSet.toMarketOrder() = MarketOrder(
        id = getLong("id"),
        purchaserId = getLong("purchaser_id"),
        producerId = getLong("producer_id"),
        status = OrderStatus.valueOf(getString("status")),
        deliveryDate = getObject("delivery_date", java.time.LocalDate::class.java),
        totalCents = getInt("total_cents"),
        notes = getString("notes"),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}

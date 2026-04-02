package app.verdant.repository

import app.verdant.entity.OrderItem
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class OrderItemRepository(private val ds: AgroalDataSource) {

    fun findByOrderId(orderId: Long): List<OrderItem> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM order_item WHERE order_id = ?").use { ps ->
                ps.setLong(1, orderId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toOrderItem()) }
                }
            }
        }

    fun persist(item: OrderItem): OrderItem {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO order_item (order_id, listing_id, species_id, species_name, quantity, price_per_stem_sek)
                   VALUES (?, ?, ?, ?, ?, ?)""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, item.orderId)
                ps.setLong(2, item.listingId)
                ps.setLong(3, item.speciesId)
                ps.setString(4, item.speciesName)
                ps.setInt(5, item.quantity)
                ps.setInt(6, item.pricePerStemSek)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return item.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun deleteByOrderId(orderId: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM order_item WHERE order_id = ?").use { ps ->
                ps.setLong(1, orderId)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toOrderItem() = OrderItem(
        id = getLong("id"),
        orderId = getLong("order_id"),
        listingId = getLong("listing_id"),
        speciesId = getLong("species_id"),
        speciesName = getString("species_name"),
        quantity = getInt("quantity"),
        pricePerStemSek = getInt("price_per_stem_sek"),
    )
}

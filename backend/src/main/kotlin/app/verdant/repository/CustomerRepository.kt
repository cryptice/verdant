package app.verdant.repository

import app.verdant.entity.Channel
import app.verdant.entity.Customer
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class CustomerRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): Customer? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM customer WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toCustomer() else null }
            }
        }

    fun findByOrgId(orgId: Long, limit: Int = 50, offset: Int = 0): List<Customer> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM customer WHERE org_id = ? ORDER BY name LIMIT ? OFFSET ?").use { ps ->
                ps.setLong(1, orgId)
                ps.setInt(2, limit)
                ps.setInt(3, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toCustomer()) }
                }
            }
        }

    fun persist(customer: Customer): Customer {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO customer (org_id, name, channel, contact_info, notes, created_at)
                   VALUES (?, ?, ?, ?, ?, now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, customer.orgId)
                ps.setString(2, customer.name)
                ps.setString(3, customer.channel.name)
                ps.setString(4, customer.contactInfo)
                ps.setString(5, customer.notes)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return customer.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(customer: Customer) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "UPDATE customer SET name = ?, channel = ?, contact_info = ?, notes = ? WHERE id = ?"
            ).use { ps ->
                ps.setString(1, customer.name)
                ps.setString(2, customer.channel.name)
                ps.setString(3, customer.contactInfo)
                ps.setString(4, customer.notes)
                ps.setLong(5, customer.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM customer WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Customer not found")
            }
        }
    }

    private fun ResultSet.toCustomer() = Customer(
        id = getLong("id"),
        orgId = getLong("org_id"),
        name = getString("name"),
        channel = Channel.valueOf(getString("channel")),
        contactInfo = getString("contact_info"),
        notes = getString("notes"),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}

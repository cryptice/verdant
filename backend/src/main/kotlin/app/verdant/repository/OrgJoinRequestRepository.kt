package app.verdant.repository

import app.verdant.entity.JoinRequestStatus
import app.verdant.entity.OrgJoinRequest
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet

@ApplicationScoped
class OrgJoinRequestRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): OrgJoinRequest? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM org_join_request WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toJoinRequest() else null }
            }
        }

    fun findPendingByOrgId(orgId: Long): List<OrgJoinRequest> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT * FROM org_join_request WHERE org_id = ? AND status = 'PENDING' ORDER BY id"
            ).use { ps ->
                ps.setLong(1, orgId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toJoinRequest()) }
                }
            }
        }

    fun findByOrgAndUser(orgId: Long, userId: Long): OrgJoinRequest? =
        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT * FROM org_join_request WHERE org_id = ? AND user_id = ?"
            ).use { ps ->
                ps.setLong(1, orgId)
                ps.setLong(2, userId)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toJoinRequest() else null }
            }
        }

    fun persist(request: OrgJoinRequest): OrgJoinRequest {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO org_join_request (org_id, user_id, status, created_at) VALUES (?, ?, ?, now())",
                arrayOf("id")
            ).use { ps ->
                ps.setLong(1, request.orgId)
                ps.setLong(2, request.userId)
                ps.setString(3, request.status.name)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return request.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun updateStatus(id: Long, status: JoinRequestStatus) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "UPDATE org_join_request SET status = ? WHERE id = ?"
            ).use { ps ->
                ps.setString(1, status.name)
                ps.setLong(2, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Join request not found")
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM org_join_request WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toJoinRequest() = OrgJoinRequest(
        id = getLong("id"),
        orgId = getLong("org_id"),
        userId = getLong("user_id"),
        status = JoinRequestStatus.valueOf(getString("status")),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}

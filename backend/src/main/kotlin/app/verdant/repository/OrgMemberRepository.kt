package app.verdant.repository

import app.verdant.entity.OrgMember
import app.verdant.entity.OrgRole
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet

@ApplicationScoped
class OrgMemberRepository(private val ds: AgroalDataSource) {

    fun findByOrgId(orgId: Long): List<OrgMember> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM org_member WHERE org_id = ? ORDER BY id").use { ps ->
                ps.setLong(1, orgId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toOrgMember()) }
                }
            }
        }

    fun findByUserId(userId: Long): List<OrgMember> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM org_member WHERE user_id = ? ORDER BY id").use { ps ->
                ps.setLong(1, userId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toOrgMember()) }
                }
            }
        }

    fun findByOrgAndUser(orgId: Long, userId: Long): OrgMember? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM org_member WHERE org_id = ? AND user_id = ?").use { ps ->
                ps.setLong(1, orgId)
                ps.setLong(2, userId)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toOrgMember() else null }
            }
        }

    fun persist(member: OrgMember): OrgMember {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO org_member (org_id, user_id, role, joined_at) VALUES (?, ?, ?, now())",
                arrayOf("id")
            ).use { ps ->
                ps.setLong(1, member.orgId)
                ps.setLong(2, member.userId)
                ps.setString(3, member.role.name)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return member.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun delete(orgId: Long, userId: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM org_member WHERE org_id = ? AND user_id = ?").use { ps ->
                ps.setLong(1, orgId)
                ps.setLong(2, userId)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Org member not found")
            }
        }
    }

    fun isMember(orgId: Long, userId: Long): Boolean =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT 1 FROM org_member WHERE org_id = ? AND user_id = ?").use { ps ->
                ps.setLong(1, orgId)
                ps.setLong(2, userId)
                ps.executeQuery().use { rs -> rs.next() }
            }
        }

    private fun ResultSet.toOrgMember() = OrgMember(
        id = getLong("id"),
        orgId = getLong("org_id"),
        userId = getLong("user_id"),
        role = OrgRole.valueOf(getString("role")),
        joinedAt = getTimestamp("joined_at").toInstant(),
    )
}

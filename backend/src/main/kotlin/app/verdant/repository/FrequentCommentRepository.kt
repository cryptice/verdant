package app.verdant.repository

import app.verdant.entity.FrequentComment
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class FrequentCommentRepository(private val ds: AgroalDataSource) {

    fun findByUserId(userId: Long): List<FrequentComment> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM frequent_comment WHERE user_id = ? ORDER BY use_count DESC").use { ps ->
                ps.setLong(1, userId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toComment()) }
                }
            }
        }

    fun recordUsage(userId: Long, text: String): FrequentComment {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO frequent_comment (user_id, text, use_count) VALUES (?, ?, 1)
                   ON CONFLICT (user_id, text) DO UPDATE SET use_count = frequent_comment.use_count + 1
                   RETURNING *"""
            ).use { ps ->
                ps.setLong(1, userId)
                ps.setString(2, text)
                ps.executeQuery().use { rs ->
                    rs.next()
                    return rs.toComment()
                }
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM frequent_comment WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toComment() = FrequentComment(
        id = getLong("id"),
        userId = getLong("user_id"),
        text = getString("text"),
        useCount = getInt("use_count"),
    )
}

package app.verdant.mcp

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

data class McpClient(val id: String, val name: String, val redirectUris: List<String>)
data class McpAuthorization(val id: UUID, val clientId: String, val redirectUri: String, val state: String?, val challenge: String, val expiresAt: Instant)
data class McpGrant(val id: UUID, val clientId: String, val userId: Long, val orgId: Long, val expiresAt: Instant, val revokedAt: Instant?)
data class McpRefresh(val grant: McpGrant, val expiresAt: Instant, val usedAt: Instant?)

@ApplicationScoped
class McpOAuthRepository(private val ds: AgroalDataSource, private val mapper: ObjectMapper) {
    fun createClient(client: McpClient) = ds.connection.use { c ->
        c.prepareStatement("INSERT INTO mcp_oauth_client(id,name,redirect_uris) VALUES (?,?,?::jsonb)").use { p ->
            p.setString(1, client.id); p.setString(2, client.name); p.setString(3, mapper.writeValueAsString(client.redirectUris)); p.executeUpdate()
        }
    }
    fun client(id: String): McpClient? = ds.connection.use { c ->
        c.prepareStatement("SELECT * FROM mcp_oauth_client WHERE id=?").use { p ->
            p.setString(1, id); p.executeQuery().use { r -> if (!r.next()) null else McpClient(r.getString("id"), r.getString("name"), mapper.readValue(r.getString("redirect_uris"), object : TypeReference<List<String>>() {})) }
        }
    }
    fun createRequest(a: McpAuthorization) = ds.connection.use { c ->
        c.prepareStatement("INSERT INTO mcp_oauth_request VALUES (?,?,?,?,?,?)").use { p ->
            p.setObject(1, a.id); p.setString(2, a.clientId); p.setString(3, a.redirectUri); p.setString(4, a.state); p.setString(5, a.challenge); p.setObject(6, a.expiresAt.atOffset(java.time.ZoneOffset.UTC)); p.executeUpdate()
        }
    }
    fun request(id: UUID, consume: Boolean = false): McpAuthorization? = ds.connection.use { c ->
        val sql = if (consume) "DELETE FROM mcp_oauth_request WHERE id=? AND expires_at>now() RETURNING *" else "SELECT * FROM mcp_oauth_request WHERE id=? AND expires_at>now()"
        c.prepareStatement(sql).use { p ->
            p.setObject(1, id); p.executeQuery().use { r -> if (!r.next()) null else McpAuthorization(id, r.getString("client_id"), r.getString("redirect_uri"), r.getString("state"), r.getString("code_challenge"), r.getTimestamp("expires_at").toInstant()) }
        }
    }
    fun createGrant(g: McpGrant) = ds.connection.use { c ->
        c.prepareStatement("INSERT INTO mcp_org_profile(org_id,profile_id) VALUES (?,?) ON CONFLICT(org_id) DO NOTHING").use { p -> p.setLong(1, g.orgId); p.setObject(2, UUID.randomUUID()); p.executeUpdate() }
        c.prepareStatement("INSERT INTO mcp_oauth_grant(id,client_id,user_id,org_id,expires_at) VALUES (?,?,?,?,?)").use { p ->
            p.setObject(1, g.id); p.setString(2, g.clientId); p.setLong(3, g.userId); p.setLong(4, g.orgId); p.setObject(5, g.expiresAt.atOffset(java.time.ZoneOffset.UTC)); p.executeUpdate()
        }
    }
    fun grant(id: UUID): McpGrant? = ds.connection.use { c ->
        c.prepareStatement("SELECT * FROM mcp_oauth_grant WHERE id=?").use { p -> p.setObject(1, id); p.executeQuery().use { r -> if (r.next()) r.grant() else null } }
    }
    fun createCode(hash: String, g: McpGrant, a: McpAuthorization) = ds.connection.use { c ->
        c.prepareStatement("INSERT INTO mcp_oauth_code VALUES (?,?,?,?,?,now()+interval '2 minutes')").use { p ->
            p.setString(1, hash); p.setObject(2, g.id); p.setString(3, g.clientId); p.setString(4, a.redirectUri); p.setString(5, a.challenge); p.executeUpdate()
        }
    }
    fun consumeCode(hash: String, client: String, redirect: String, challenge: String): UUID? = ds.connection.use { c ->
        c.prepareStatement("DELETE FROM mcp_oauth_code WHERE code_hash=? AND client_id=? AND redirect_uri=? AND code_challenge=? AND expires_at>now() RETURNING grant_id").use { p ->
            p.setString(1, hash); p.setString(2, client); p.setString(3, redirect); p.setString(4, challenge); p.executeQuery().use { r -> if (r.next()) r.getObject(1, UUID::class.java) else null }
        }
    }
    fun createRefresh(hash: String, g: McpGrant) = ds.connection.use { c ->
        c.prepareStatement("INSERT INTO mcp_oauth_refresh(token_hash,grant_id,expires_at) VALUES (?,?,now()+interval '30 days')").use { p -> p.setString(1, hash); p.setObject(2, g.id); p.executeUpdate() }
        c.prepareStatement("UPDATE mcp_oauth_grant SET expires_at=now()+interval '30 days' WHERE id=?").use { p -> p.setObject(1, g.id); p.executeUpdate() }
    }
    fun refresh(hash: String): McpRefresh? = ds.connection.use { c ->
        c.prepareStatement("SELECT g.*,r.expires_at AS refresh_expires,r.used_at FROM mcp_oauth_refresh r JOIN mcp_oauth_grant g ON g.id=r.grant_id WHERE r.token_hash=? FOR UPDATE OF r,g").use { p ->
            p.setString(1, hash); p.executeQuery().use { r -> if (r.next()) McpRefresh(r.grant(), r.getTimestamp("refresh_expires").toInstant(), r.getTimestamp("used_at")?.toInstant()) else null }
        }
    }
    fun markRefreshUsed(hash: String) = ds.connection.use { c ->
        c.prepareStatement("UPDATE mcp_oauth_refresh SET used_at=now() WHERE token_hash=?").use { p -> p.setString(1, hash); p.executeUpdate() }
    }
    fun revoke(id: UUID) = ds.connection.use { c ->
        c.prepareStatement("UPDATE mcp_oauth_grant SET revoked_at=COALESCE(revoked_at,now()) WHERE id=?").use { p -> p.setObject(1, id); p.executeUpdate() }
    }
    fun connections(userId: Long): List<Map<String, Any>> = ds.connection.use { c ->
        c.prepareStatement("SELECT g.id,c.name AS client_name,o.name AS org_name,g.created_at,g.expires_at FROM mcp_oauth_grant g JOIN mcp_oauth_client c ON c.id=g.client_id JOIN organization o ON o.id=g.org_id WHERE g.user_id=? AND g.revoked_at IS NULL AND g.expires_at>now() ORDER BY g.created_at DESC").use { p ->
            p.setLong(1, userId); p.executeQuery().use { r -> buildList { while (r.next()) add(mapOf("id" to r.getObject("id").toString(), "clientName" to r.getString("client_name"), "organizationName" to r.getString("org_name"), "createdAt" to r.getTimestamp("created_at").toInstant(), "expiresAt" to r.getTimestamp("expires_at").toInstant())) } }
        }
    }
    fun profile(orgId: Long): String? = ds.connection.use { c ->
        c.prepareStatement("SELECT profile_id FROM mcp_org_profile WHERE org_id=?").use { p -> p.setLong(1, orgId); p.executeQuery().use { r -> if (r.next()) r.getObject(1).toString() else null } }
    }
    fun cleanExpired() = ds.connection.use { c ->
        for (sql in listOf(
            "DELETE FROM mcp_oauth_request WHERE expires_at < now()",
            "DELETE FROM mcp_oauth_code WHERE expires_at < now()",
            "DELETE FROM mcp_oauth_refresh WHERE expires_at < now()",
            "DELETE FROM mcp_oauth_grant WHERE expires_at < now() OR revoked_at < now()-interval '30 days'",
            "DELETE FROM mcp_oauth_client c WHERE created_at < now()-interval '90 days' AND NOT EXISTS (SELECT 1 FROM mcp_oauth_grant g WHERE g.client_id=c.id) AND NOT EXISTS (SELECT 1 FROM mcp_oauth_request r WHERE r.client_id=c.id)",
        )) c.prepareStatement(sql).use { it.executeUpdate() }
    }
    private fun ResultSet.grant() = McpGrant(getObject("id", UUID::class.java), getString("client_id"), getLong("user_id"), getLong("org_id"), getTimestamp("expires_at").toInstant(), getTimestamp("revoked_at")?.toInstant())
}

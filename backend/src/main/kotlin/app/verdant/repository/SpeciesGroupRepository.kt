package app.verdant.repository

import app.verdant.entity.SpeciesGroup
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class SpeciesGroupRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): SpeciesGroup? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_group WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toGroup() else null }
            }
        }

    fun findAll(): List<SpeciesGroup> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_group ORDER BY name").use { ps ->
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toGroup()) }
                }
            }
        }

    fun findNamesByIds(ids: Set<Long>): Map<Long, String> {
        if (ids.isEmpty()) return emptyMap()
        val placeholders = ids.joinToString(",") { "?" }
        return ds.connection.use { conn ->
            conn.prepareStatement("SELECT id, name FROM species_group WHERE id IN ($placeholders)").use { ps ->
                ids.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    buildMap { while (rs.next()) put(rs.getLong("id"), rs.getString("name")) }
                }
            }
        }
    }

    fun findByOrgId(orgId: Long): List<SpeciesGroup> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_group WHERE org_id = ? OR org_id IS NULL ORDER BY name").use { ps ->
                ps.setLong(1, orgId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toGroup()) }
                }
            }
        }

    fun persist(group: SpeciesGroup): SpeciesGroup {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO species_group (org_id, name) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                if (group.orgId != null) ps.setLong(1, group.orgId) else ps.setNull(1, java.sql.Types.BIGINT)
                ps.setString(2, group.name)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return group.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(group: SpeciesGroup) {
        ds.connection.use { conn ->
            conn.prepareStatement("UPDATE species_group SET name = ? WHERE id = ?").use { ps ->
                ps.setString(1, group.name)
                ps.setLong(2, group.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM species_group WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Species group not found")
            }
        }
    }

    fun findGroupIdsBySpeciesId(speciesId: Long): List<Long> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT group_id FROM species_group_membership WHERE species_id = ?").use { ps ->
                ps.setLong(1, speciesId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.getLong("group_id")) }
                }
            }
        }

    fun findGroupIdsBySpeciesIds(speciesIds: Set<Long>): Map<Long, List<Long>> {
        if (speciesIds.isEmpty()) return emptyMap()
        val placeholders = speciesIds.joinToString(",") { "?" }
        return ds.connection.use { conn ->
            conn.prepareStatement("SELECT species_id, group_id FROM species_group_membership WHERE species_id IN ($placeholders)").use { ps ->
                speciesIds.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    val result = mutableMapOf<Long, MutableList<Long>>()
                    while (rs.next()) {
                        result.getOrPut(rs.getLong("species_id")) { mutableListOf() }
                            .add(rs.getLong("group_id"))
                    }
                    result
                }
            }
        }
    }

    fun findSpeciesIdsByGroupId(groupId: Long): List<Long> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT species_id FROM species_group_membership WHERE group_id = ?").use { ps ->
                ps.setLong(1, groupId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.getLong("species_id")) }
                }
            }
        }

    fun addSpeciesToGroup(speciesId: Long, groupId: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO species_group_membership (species_id, group_id) VALUES (?, ?) ON CONFLICT DO NOTHING"
            ).use { ps ->
                ps.setLong(1, speciesId)
                ps.setLong(2, groupId)
                ps.executeUpdate()
            }
        }
    }

    fun removeSpeciesFromGroup(speciesId: Long, groupId: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM species_group_membership WHERE species_id = ? AND group_id = ?").use { ps ->
                ps.setLong(1, speciesId)
                ps.setLong(2, groupId)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toGroup() = SpeciesGroup(
        id = getLong("id"),
        orgId = getObject("org_id") as? Long,
        name = getString("name"),
    )
}

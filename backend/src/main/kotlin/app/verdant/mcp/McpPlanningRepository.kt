package app.verdant.mcp

import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate

data class PlannedStems(val speciesId: Long, val date: LocalDate, val quantity: Long)

@ApplicationScoped
class McpPlanningRepository(private val ds: AgroalDataSource) {
    fun plannedStems(orgId: Long, seasonId: Long): List<PlannedStems> = ds.connection.use { c ->
        c.prepareStatement("""SELECT (a->>'speciesId')::bigint AS species_id,
            (p.request->>'harvestDate')::date AS harvest_date, SUM((a->>'outputQuantity')::bigint) AS quantity
            FROM harvest_plan p CROSS JOIN LATERAL jsonb_array_elements(p.snapshot->'allocations') a
            WHERE p.org_id=? AND p.season_id=? AND p.status='ACTIVE' AND p.request->>'sellableUnit'='STEM'
            GROUP BY species_id, harvest_date""").use { p ->
            p.setLong(1, orgId); p.setLong(2, seasonId)
            p.executeQuery().use { r -> buildList { while (r.next()) add(PlannedStems(r.getLong("species_id"), r.getDate("harvest_date").toLocalDate(), r.getLong("quantity"))) } }
        }
    }
}

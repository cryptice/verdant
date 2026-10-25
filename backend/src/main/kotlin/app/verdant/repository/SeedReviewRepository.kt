package app.verdant.repository

import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.time.LocalDate

data class SoldFigures(val stems: Int, val revenueCents: Long)

/**
 * Per-species aggregates for the seed season review. Date-based figures take the
 * season's effective date ranges; an empty list of ranges matches nothing.
 */
@ApplicationScoped
class SeedReviewRepository(private val ds: AgroalDataSource) {

    /** Plants sown from seed (not registered) with a sow date in the ranges. */
    fun sownBySpecies(orgId: Long, ranges: List<ClosedRange<LocalDate>>): Map<Long, Int> =
        byRanges(ranges) {
            query(
                """SELECT species_id, COUNT(*) FROM plant
                   WHERE org_id = ? AND species_id IS NOT NULL AND origin = 'SOWN' AND ${rangeClause("planted_date", ranges)}
                   GROUP BY species_id""",
                listOf(orgId) + rangeParams(ranges),
            ) { it.getInt(2) }
        }

    /** Distinct plants per species and event type with an event dated in the ranges; registered plants were not sown, so they are left out of the seeded step. */
    fun eventPlantCountsBySpecies(orgId: Long, ranges: List<ClosedRange<LocalDate>>): Map<Long, Map<String, Int>> =
        byRanges(ranges) {
            ds.connection.use { conn ->
                conn.prepareStatement(
                    """SELECT p.species_id, e.event_type, COUNT(DISTINCT e.plant_id)
                       FROM plant_event e JOIN plant p ON p.id = e.plant_id
                       WHERE p.org_id = ? AND p.species_id IS NOT NULL AND ${rangeClause("e.event_date", ranges)}
                         AND NOT (e.event_type = 'SEEDED' AND p.origin = 'REGISTERED')
                       GROUP BY p.species_id, e.event_type"""
                ).use { ps ->
                    bind(ps, listOf(orgId) + rangeParams(ranges))
                    ps.executeQuery().use { rs ->
                        val result = mutableMapOf<Long, MutableMap<String, Int>>()
                        while (rs.next()) result.getOrPut(rs.getLong(1)) { mutableMapOf() }[rs.getString(2)] = rs.getInt(3)
                        result
                    }
                }
            }
        }

    /** Stems (or quantity) on harvest events dated in the ranges, whenever the plant was sown. */
    fun harvestedStemsBySpecies(orgId: Long, ranges: List<ClosedRange<LocalDate>>): Map<Long, Int> =
        byRanges(ranges) {
            query(
                """SELECT p.species_id, SUM(COALESCE(e.stem_count, e.quantity, 0))
                   FROM plant_event e JOIN plant p ON p.id = e.plant_id
                   WHERE p.org_id = ? AND p.species_id IS NOT NULL AND e.event_type = 'HARVESTED'
                     AND ${rangeClause("e.event_date", ranges)}
                   GROUP BY p.species_id""",
                listOf(orgId) + rangeParams(ranges),
            ) { it.getInt(2) }
        }

    /** Sales dated in the ranges from plant and harvest lots; bouquet lots mix species and are left out. */
    fun salesBySpecies(orgId: Long, ranges: List<ClosedRange<LocalDate>>): Map<Long, SoldFigures> =
        byRanges(ranges) {
            query(
                """SELECT p.species_id,
                          SUM(s.quantity * COALESCE(l.stems_per_unit, 1)),
                          SUM(s.quantity::bigint * s.price_per_unit_cents)
                   FROM sale s
                   JOIN sale_lot l ON l.id = s.sale_lot_id
                   LEFT JOIN plant_event he ON he.id = l.harvest_event_id
                   JOIN plant p ON p.id = COALESCE(l.plant_id, he.plant_id)
                   WHERE l.org_id = ? AND l.source_kind IN ('PLANT', 'HARVEST_EVENT') AND p.species_id IS NOT NULL
                     AND ${rangeClause("s.sold_at", ranges)}
                   GROUP BY p.species_id""",
                listOf(orgId) + rangeParams(ranges),
            ) { SoldFigures(it.getInt(2), it.getLong(3)) }
        }

    /** Seed currently held in the organization's batches. */
    fun stockBySpecies(orgId: Long): Map<Long, Int> =
        query(
            "SELECT species_id, SUM(quantity) FROM seed_inventory WHERE org_id = ? GROUP BY species_id",
            listOf(orgId),
        ) { it.getInt(2) }

    /** Average cost per unit across the species' batches that have a cost. */
    fun averageUnitCostBySpecies(orgId: Long): Map<Long, Double> =
        query(
            """SELECT species_id, AVG(cost_per_unit_cents) FROM seed_inventory
               WHERE org_id = ? AND cost_per_unit_cents IS NOT NULL GROUP BY species_id""",
            listOf(orgId),
        ) { it.getDouble(2) }

    /** Seed added to stock on dates in the ranges. Opening balances are not purchases and never count. */
    fun addedBySpecies(orgId: Long, ranges: List<ClosedRange<LocalDate>>): Map<Long, Int> =
        byRanges(ranges) {
            query(
                """SELECT species_id, SUM(quantity) FROM seed_stock_movement
                   WHERE org_id = ? AND kind = 'ADDED' AND ${rangeClause("occurred_on", ranges)} GROUP BY species_id""",
                listOf(orgId) + rangeParams(ranges),
            ) { it.getInt(2) }
        }

    /** Cost of the seed drawn for sowings dated in the ranges, at each movement's recorded unit cost. */
    fun sownCostBySpecies(orgId: Long, ranges: List<ClosedRange<LocalDate>>): Map<Long, Long> =
        byRanges(ranges) {
            query(
                """SELECT species_id, SUM(quantity::bigint * COALESCE(unit_cost_cents, 0)) FROM seed_stock_movement
                   WHERE org_id = ? AND kind = 'SOWN' AND ${rangeClause("occurred_on", ranges)} GROUP BY species_id""",
                listOf(orgId) + rangeParams(ranges),
            ) { it.getLong(2) }
        }

    private fun <T> byRanges(ranges: List<ClosedRange<LocalDate>>, block: () -> Map<Long, T>): Map<Long, T> =
        if (ranges.isEmpty()) emptyMap() else block()

    private fun rangeClause(column: String, ranges: List<ClosedRange<LocalDate>>): String =
        ranges.joinToString(" OR ", prefix = "(", postfix = ")") { "$column BETWEEN ? AND ?" }

    private fun rangeParams(ranges: List<ClosedRange<LocalDate>>): List<Any> =
        ranges.flatMap { listOf(it.start, it.endInclusive) }

    private fun bind(ps: java.sql.PreparedStatement, params: List<Any>) =
        params.forEachIndexed { i, p -> ps.setObject(i + 1, p) }

    private fun <T> query(sql: String, params: List<Any>, value: (ResultSet) -> T): Map<Long, T> =
        ds.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                bind(ps, params)
                ps.executeQuery().use { rs ->
                    buildMap { while (rs.next()) put(rs.getLong(1), value(rs)) }
                }
            }
        }
}

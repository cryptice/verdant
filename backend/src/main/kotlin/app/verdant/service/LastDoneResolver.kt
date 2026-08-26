package app.verdant.service

import app.verdant.entity.MaintenanceActivity
import app.verdant.entity.MaintenanceRule
import app.verdant.entity.MaintenanceTarget
import app.verdant.repository.GardenAreaEventRepository
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.time.ZoneId

/**
 * Works out when a rule's activity was last carried out.
 *
 * Deliberately derived rather than stamped on the rule: weeding a path and
 * logging it the ordinary way must reset the reminder, whether the work was
 * logged by completing a task or by pressing the button on the detail screen.
 */
@ApplicationScoped
class LastDoneResolver(
    private val ds: AgroalDataSource,
    private val areaEvents: GardenAreaEventRepository,
) {
    fun resolve(rule: MaintenanceRule): LocalDate? = when (rule.target) {
        MaintenanceTarget.GARDEN_AREA -> areaEvents.findLatestDate(rule.gardenAreaId!!, rule.activity.name)
        MaintenanceTarget.BED -> resolveForBed(rule.bedId!!, rule.activity)
    }

    private fun resolveForBed(bedId: Long, activity: MaintenanceActivity): LocalDate? {
        val fromEvents = latestBedEvent(bedId, activity.bedEventType!!.name)
        // Bed fertilising is recorded in supply_application, not bed_event, so a
        // FERTILIZE rule has to look at both or its clock would never move.
        val fromApplications =
            if (activity == MaintenanceActivity.FERTILIZE) latestFertilizerApplication(bedId) else null
        return listOfNotNull(fromEvents, fromApplications).maxOrNull()
    }

    private fun latestBedEvent(bedId: Long, eventType: String): LocalDate? = ds.connection.use { conn ->
        conn.prepareStatement(
            "SELECT MAX(event_date) AS latest FROM bed_event WHERE bed_id = ? AND event_type = ?"
        ).use { ps ->
            ps.setLong(1, bedId)
            ps.setString(2, eventType)
            ps.executeQuery().use { rs -> if (rs.next()) rs.getDate("latest")?.toLocalDate() else null }
        }
    }

    private fun latestFertilizerApplication(bedId: Long): LocalDate? = ds.connection.use { conn ->
        conn.prepareStatement(
            """SELECT MAX(sa.applied_at) AS latest
               FROM supply_application sa
               JOIN supply_type st ON sa.supply_type_id = st.id
               WHERE sa.bed_id = ? AND st.category = 'FERTILIZER'"""
        ).use { ps ->
            ps.setLong(1, bedId)
            ps.executeQuery().use { rs ->
                // Converted JVM-side (rather than sa.applied_at::date in SQL) to match
                // SupplyApplicationService's precedent and avoid depending on the
                // Postgres session timezone, which nothing in this repo pins.
                if (rs.next()) {
                    rs.getTimestamp("latest")?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
                } else {
                    null
                }
            }
        }
    }
}

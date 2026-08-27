package app.verdant.service

import app.verdant.entity.ScheduledTask
import app.verdant.repository.MaintenanceRuleRepository
import app.verdant.repository.ScheduledTaskRepository
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import org.slf4j.LoggerFactory
import java.time.LocalDate

/**
 * Turns due maintenance rules into ordinary scheduled tasks once a day, so
 * recurring work shows up in the same task list as everything else.
 *
 * The only creator of rule-backed tasks. Completing a task does not chain
 * synchronously into the next one — the next run picks it up once the derived
 * last-done date has moved.
 */
@ApplicationScoped
class MaintenanceScheduler(
    private val rules: MaintenanceRuleRepository,
    private val lastDoneResolver: LastDoneResolver,
    private val tasks: ScheduledTaskRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 30 3 * * ?")
    fun materialiseDueTasks() {
        val created = run(LocalDate.now())
        if (created > 0) log.info("Maintenance scheduler created $created task(s)")
    }

    /**
     * Visible for testing so the date can be driven directly.
     * Returns how many tasks were created.
     */
    internal fun run(today: LocalDate): Int {
        var created = 0
        for (rule in rules.findActiveWithoutOpenTask()) {
            // A rule whose target vanished mid-run, or whose lookup fails, must
            // not stop the rest of the garden from being scheduled.
            runCatching {
                val due = MaintenanceDueCalculator.dueDate(
                    lastDone = MaintenanceDueCalculator.effectiveLastDone(
                        rule, lastDoneResolver.resolve(rule),
                    ),
                    intervalDays = rule.intervalDays,
                    window = MaintenanceDueCalculator.windowOf(rule),
                    today = today,
                )
                if (due <= today) {
                    tasks.persist(
                        ScheduledTask(
                            orgId = rule.orgId,
                            speciesId = null,
                            bedId = rule.bedId,
                            gardenAreaId = rule.gardenAreaId,
                            maintenanceRuleId = rule.id,
                            activityType = rule.activity.name,
                            earliestDate = due,
                            deadline = due,
                            targetCount = 1,
                            remainingCount = 1,
                        )
                    )
                    created++
                }
            }.onFailure {
                log.warn(
                    "Maintenance rule ${rule.id} (org=${rule.orgId}, activity=${rule.activity}) skipped",
                    it,
                )
            }
        }
        return created
    }
}

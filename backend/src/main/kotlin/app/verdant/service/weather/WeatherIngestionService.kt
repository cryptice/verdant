package app.verdant.service.weather

import app.verdant.repository.DailyWeatherRepository
import app.verdant.repository.GardenRepository
import io.quarkus.scheduler.Scheduled
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ApplicationScoped
class WeatherIngestionService(
    private val gardens: GardenRepository,
    private val weather: DailyWeatherRepository,
    private val smhi: SmhiClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Single-threaded executor for async backfill. ManagedExecutor (microprofile-context-propagation)
    // is not on the classpath in M1; revisit if context propagation is needed.
    private val backfillExecutor = Executors.newSingleThreadExecutor()

    // Throttles between upstream requests. Exposed as internal vars so tests can zero them out.
    internal var forecastThrottleMs: Long = 250
    internal var actualsThrottleMs: Long = 250
    internal var backfillThrottleMs: Long = 25
    internal var backfillYears: Long = 3

    @Scheduled(cron = "0 0 */6 * * ?")
    fun refreshForecasts() {
        val all = gardens.findAllWithCoordinates()
        log.info("Weather forecast refresh starting for ${all.size} gardens")
        for (g in all) {
            runCatching {
                val rows = smhi.fetchForecast(g.latitude!!, g.longitude!!, g.id!!)
                if (rows.isNotEmpty()) {
                    weather.upsert(rows)
                }
                evaluateAlerts(g.id)
                if (forecastThrottleMs > 0) Thread.sleep(forecastThrottleMs)
            }.onFailure { log.warn("Forecast refresh failed for garden ${g.id}", it) }
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    fun refreshActuals() {
        val yesterday = LocalDate.now().minusDays(1)
        val all = gardens.findAllWithCoordinates()
        log.info("Weather actuals refresh starting for ${all.size} gardens (date=$yesterday)")
        for (g in all) {
            runCatching {
                val row = smhi.fetchActual(g.latitude!!, g.longitude!!, g.id!!, yesterday) ?: return@runCatching
                weather.upsert(listOf(row))
                if (actualsThrottleMs > 0) Thread.sleep(actualsThrottleMs)
            }.onFailure { log.warn("Actuals refresh failed for garden ${g.id}", it) }
        }
    }

    fun backfillForGarden(gardenId: Long) {
        val g = gardens.findById(gardenId) ?: return
        val lat = g.latitude ?: return
        val lon = g.longitude ?: return
        gardens.setBackfillStatus(gardenId, "RUNNING")
        runCatching {
            val start = LocalDate.now().minusYears(backfillYears)
            val end = LocalDate.now().minusDays(1)
            val rows = mutableListOf<app.verdant.entity.DailyWeather>()
            var d = start
            while (!d.isAfter(end) && !Thread.currentThread().isInterrupted) {
                val row = smhi.fetchActual(lat, lon, gardenId, d)
                if (row != null) rows += row
                if (rows.size >= 500) { weather.upsert(rows); rows.clear() }
                d = d.plusDays(1)
                if (backfillThrottleMs > 0) {
                    try {
                        Thread.sleep(backfillThrottleMs)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw e
                    }
                }
            }
            if (rows.isNotEmpty()) weather.upsert(rows)
            gardens.setBackfillStatus(gardenId, "DONE")
        }.onFailure {
            log.error("Backfill failed for garden $gardenId", it)
            gardens.setBackfillStatus(gardenId, "FAILED")
        }
    }

    fun submitBackfill(gardenId: Long) {
        backfillExecutor.submit { backfillForGarden(gardenId) }
    }

    // TODO(M3): wire AlertEvaluator bean and call evaluate(gardenId) here.
    private fun evaluateAlerts(gardenId: Long) { /* wired in M3 */ }

    @PreDestroy
    fun shutdown() {
        backfillExecutor.shutdown()
        if (!backfillExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
            log.warn("Backfill executor did not terminate in 30s; forcing shutdown")
            backfillExecutor.shutdownNow()
        }
    }
}

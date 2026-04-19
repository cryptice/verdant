package app.verdant.repository

import app.verdant.entity.DailyWeather
import app.verdant.entity.Garden
import app.verdant.entity.ObservationType
import app.verdant.entity.Organization
import io.agroal.api.AgroalDataSource
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@QuarkusTest
class DailyWeatherRepositoryTest {

    @Inject
    lateinit var repo: DailyWeatherRepository

    @Inject
    lateinit var gardenRepo: GardenRepository

    @Inject
    lateinit var orgRepo: OrganizationRepository

    @Inject
    lateinit var ds: AgroalDataSource

    private var gardenId: Long = 0
    private var otherGardenId: Long = 0

    @BeforeEach
    fun setup() {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM daily_weather").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM garden").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM organization").use { it.executeUpdate() }
        }
        val org = orgRepo.persist(Organization(name = "Test Org"))
        val garden = gardenRepo.persist(Garden(name = "Test Garden", orgId = org.id!!))
        gardenId = garden.id!!
        val otherGarden = gardenRepo.persist(Garden(name = "Other Garden", orgId = org.id))
        otherGardenId = otherGarden.id!!
    }

    @Test
    fun `upsert inserts new row`() {
        val date = LocalDate.of(2024, 6, 1)
        val row = DailyWeather(
            gardenId = gardenId,
            date = date,
            observationType = ObservationType.ACTUAL,
            tempMinC = 10.0,
            tempMaxC = 20.0,
            tempMeanC = 15.0,
            precipitationMm = 2.5,
            windMaxMs = 5.0,
            humidityPct = 70.0,
        )

        repo.upsert(listOf(row))

        val results = repo.findByGarden(gardenId, date, date)
        assertEquals(1, results.size)
        val saved = results.first()
        assertNotNull(saved.id)
        assertEquals(gardenId, saved.gardenId)
        assertEquals(date, saved.date)
        assertEquals(ObservationType.ACTUAL, saved.observationType)
        assertEquals(10.0, saved.tempMinC)
        assertEquals(20.0, saved.tempMaxC)
        assertEquals(15.0, saved.tempMeanC)
        assertEquals(2.5, saved.precipitationMm)
        assertEquals(5.0, saved.windMaxMs)
        assertEquals(70.0, saved.humidityPct)
    }

    @Test
    fun `upsert on conflict replaces values`() {
        val date = LocalDate.of(2024, 6, 2)
        val original = DailyWeather(
            gardenId = gardenId,
            date = date,
            observationType = ObservationType.FORECAST,
            tempMinC = 5.0,
            tempMaxC = 15.0,
            tempMeanC = 10.0,
            precipitationMm = null,
            windMaxMs = null,
            humidityPct = null,
        )
        repo.upsert(listOf(original))

        val updated = original.copy(tempMinC = 8.0, tempMaxC = 18.0, tempMeanC = 13.0, precipitationMm = 1.0)
        repo.upsert(listOf(updated))

        val results = repo.findByGarden(gardenId, date, date)
        assertEquals(1, results.size)
        val saved = results.first()
        assertEquals(8.0, saved.tempMinC)
        assertEquals(18.0, saved.tempMaxC)
        assertEquals(13.0, saved.tempMeanC)
        assertEquals(1.0, saved.precipitationMm)
    }

    @Test
    fun `ACTUAL and FORECAST coexist for same date`() {
        val date = LocalDate.of(2024, 6, 3)
        val actual = DailyWeather(
            gardenId = gardenId,
            date = date,
            observationType = ObservationType.ACTUAL,
            tempMinC = 12.0,
            tempMaxC = 22.0,
            tempMeanC = 17.0,
            precipitationMm = null,
            windMaxMs = null,
            humidityPct = null,
        )
        val forecast = DailyWeather(
            gardenId = gardenId,
            date = date,
            observationType = ObservationType.FORECAST,
            tempMinC = 11.0,
            tempMaxC = 21.0,
            tempMeanC = 16.0,
            precipitationMm = null,
            windMaxMs = null,
            humidityPct = null,
        )

        repo.upsert(listOf(actual, forecast))

        val results = repo.findByGarden(gardenId, date, date)
        assertEquals(2, results.size)
        assertEquals(ObservationType.ACTUAL, results[0].observationType)
        assertEquals(ObservationType.FORECAST, results[1].observationType)
    }

    @Test
    fun `findByGarden respects date range`() {
        val dates = listOf(
            LocalDate.of(2024, 6, 1),
            LocalDate.of(2024, 6, 5),
            LocalDate.of(2024, 6, 10),
        )
        repo.upsert(dates.map { date ->
            DailyWeather(
                gardenId = gardenId,
                date = date,
                observationType = ObservationType.ACTUAL,
                tempMinC = null, tempMaxC = null, tempMeanC = null,
                precipitationMm = null, windMaxMs = null, humidityPct = null,
            )
        })

        val results = repo.findByGarden(gardenId, LocalDate.of(2024, 6, 3), LocalDate.of(2024, 6, 8))
        assertEquals(1, results.size)
        assertEquals(LocalDate.of(2024, 6, 5), results.first().date)
    }

    @Test
    fun `deleteByGarden deletes all for that garden only`() {
        val date = LocalDate.of(2024, 6, 1)
        repo.upsert(listOf(
            DailyWeather(
                gardenId = gardenId,
                date = date,
                observationType = ObservationType.ACTUAL,
                tempMinC = null, tempMaxC = null, tempMeanC = null,
                precipitationMm = null, windMaxMs = null, humidityPct = null,
            ),
            DailyWeather(
                gardenId = otherGardenId,
                date = date,
                observationType = ObservationType.ACTUAL,
                tempMinC = null, tempMaxC = null, tempMeanC = null,
                precipitationMm = null, windMaxMs = null, humidityPct = null,
            ),
        ))

        repo.deleteByGarden(gardenId)

        val deletedResults = repo.findByGarden(gardenId, date, date)
        assertEquals(0, deletedResults.size)

        val survivingResults = repo.findByGarden(otherGardenId, date, date)
        assertEquals(1, survivingResults.size)
    }
}

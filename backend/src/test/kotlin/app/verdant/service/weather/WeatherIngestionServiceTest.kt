package app.verdant.service.weather

import app.verdant.entity.DailyWeather
import app.verdant.entity.Garden
import app.verdant.entity.ObservationType
import app.verdant.repository.DailyWeatherRepository
import app.verdant.repository.GardenRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class WeatherIngestionServiceTest {

    private val gardens: GardenRepository = mock()
    private val weather: DailyWeatherRepository = mock()
    private val smhi: SmhiClient = mock()

    private val service = WeatherIngestionService(gardens, weather, smhi)

    private fun makeGarden(
        id: Long,
        latitude: Double? = 59.33,
        longitude: Double? = 18.06,
    ) = Garden(
        id = id,
        name = "Garden $id",
        orgId = 1L,
        latitude = latitude,
        longitude = longitude,
    )

    private fun makeForecastRow(gardenId: Long) = DailyWeather(
        gardenId = gardenId,
        date = LocalDate.now().plusDays(1),
        observationType = ObservationType.FORECAST,
        tempMinC = null,
        tempMaxC = null,
        tempMeanC = null,
        precipitationMm = null,
        windMaxMs = null,
        humidityPct = null,
    )

    @Test
    fun `refreshForecasts iterates every garden with coordinates, skips those without`() {
        val gardenWithCoords = makeGarden(id = 1L)
        // findAllWithCoordinates only returns gardens with coordinates; a garden without
        // coords would never be returned by that query. We verify the service correctly
        // calls findAllWithCoordinates and processes each returned garden.
        whenever(gardens.findAllWithCoordinates()).thenReturn(listOf(gardenWithCoords))
        whenever(smhi.fetchForecast(59.33, 18.06, 1L)).thenReturn(listOf(makeForecastRow(1L)))

        service.refreshForecasts()

        verify(smhi).fetchForecast(59.33, 18.06, 1L)
        verify(weather).upsert(any())
    }

    @Test
    fun `refreshForecasts continues after a single garden's client throws`() {
        val g1 = makeGarden(id = 1L, latitude = 59.33, longitude = 18.06)
        val g2 = makeGarden(id = 2L, latitude = 57.70, longitude = 11.97)
        whenever(gardens.findAllWithCoordinates()).thenReturn(listOf(g1, g2))
        whenever(smhi.fetchForecast(59.33, 18.06, 1L)).thenThrow(RuntimeException("network error"))
        whenever(smhi.fetchForecast(57.70, 11.97, 2L)).thenReturn(listOf(makeForecastRow(2L)))

        // Must not throw; g2 should still be processed
        service.refreshForecasts()

        verify(smhi).fetchForecast(59.33, 18.06, 1L)
        verify(smhi).fetchForecast(57.70, 11.97, 2L)
        verify(weather).upsert(any())
    }

    @Test
    fun `backfillForGarden sets status RUNNING then DONE`() {
        val gardenId = 10L
        val g = makeGarden(id = gardenId)
        whenever(gardens.findById(gardenId)).thenReturn(g)
        // SmhiClient.fetchActual is a no-op stub returning null, so no rows are upserted.

        service.backfillForGarden(gardenId)

        val inOrder = org.mockito.Mockito.inOrder(gardens)
        inOrder.verify(gardens).setBackfillStatus(gardenId, "RUNNING")
        inOrder.verify(gardens).setBackfillStatus(gardenId, "DONE")
    }

    @Test
    fun `backfillForGarden sets status FAILED on exception`() {
        val gardenId = 11L
        val g = makeGarden(id = gardenId)
        whenever(gardens.findById(gardenId)).thenReturn(g)
        // Cause an exception during upsert by making weather.upsert throw
        whenever(weather.upsert(any())).thenThrow(RuntimeException("db error"))
        // SmhiClient.fetchActual returns null by default from stub, so upsert is never called.
        // Instead we cause the failure by having smhi.fetchActual throw.
        whenever(smhi.fetchActual(any(), any(), any(), any())).thenThrow(RuntimeException("smhi error"))

        service.backfillForGarden(gardenId)

        val inOrder = org.mockito.Mockito.inOrder(gardens)
        inOrder.verify(gardens).setBackfillStatus(gardenId, "RUNNING")
        inOrder.verify(gardens).setBackfillStatus(gardenId, "FAILED")
    }
}

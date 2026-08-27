package app.verdant.service

import app.verdant.dto.UpdateBedRequest
import app.verdant.entity.Bed
import app.verdant.entity.CompassDirection
import app.verdant.entity.Drainage
import app.verdant.entity.Garden
import app.verdant.entity.IrrigationType
import app.verdant.entity.Protection
import app.verdant.entity.SoilType
import app.verdant.entity.SunExposure
import app.verdant.repository.BedPhotoRepository
import app.verdant.repository.BedRepository
import app.verdant.repository.GardenRepository
import app.verdant.repository.PlantEventRepository
import io.agroal.api.AgroalDataSource
import jakarta.ws.rs.BadRequestException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class BedServiceTest {

    private val beds: BedRepository = mock()
    private val gardens: GardenRepository = mock()
    private val photos: BedPhotoRepository = mock()
    private val storage: StorageService = mock()
    private val plantEvents: PlantEventRepository = mock()
    private val ds: AgroalDataSource = mock()
    private val service = BedService(beds, gardens, photos, storage, plantEvents, ds)

    private val orgId = 10L
    private val gardenId = 1L
    private val bedId = 3L

    private val garden = Garden(id = gardenId, name = "Trädgården", orgId = orgId)

    /** A bed with every optional field populated, so each can be cleared in turn. */
    private fun fullBed() = Bed(
        id = bedId, name = "Bädd 1", gardenId = gardenId,
        description = "Vid muren",
        lengthMeters = 4.0, widthMeters = 1.2,
        soilType = SoilType.SANDY, soilPh = 6.5,
        sunExposure = SunExposure.FULL_SUN, drainage = Drainage.GOOD,
        sunDirections = listOf(CompassDirection.S, CompassDirection.SW),
        irrigationType = IrrigationType.DRIP, protection = Protection.OPEN_FIELD,
    )

    private fun stubBed(bed: Bed = fullBed()) {
        whenever(beds.findById(bedId)).thenReturn(bed)
        whenever(gardens.findById(gardenId)).thenReturn(garden)
    }

    @Test
    fun `every optional field can be emptied with its flag`() {
        stubBed()

        val result = service.updateBed(
            bedId,
            UpdateBedRequest(
                clearLengthMeters = true, clearWidthMeters = true,
                clearSoilType = true, clearSoilPh = true,
                clearSunExposure = true, clearDrainage = true,
                clearSunDirections = true,
                clearIrrigationType = true, clearProtection = true,
            ),
            orgId,
        )

        assertEquals(null, result.lengthMeters)
        assertEquals(null, result.widthMeters)
        assertEquals(null, result.soilType)
        assertEquals(null, result.soilPh)
        assertEquals(null, result.sunExposure)
        assertEquals(null, result.drainage)
        assertEquals(emptyList<String>(), result.sunDirections)
        assertEquals(null, result.irrigationType)
        assertEquals(null, result.protection)
    }

    @Test
    fun `an update without flags leaves every optional field intact`() {
        stubBed()

        val result = service.updateBed(bedId, UpdateBedRequest(name = "Nytt namn"), orgId)

        assertEquals("Nytt namn", result.name)
        assertEquals(4.0, result.lengthMeters)
        assertEquals("SANDY", result.soilType)
        assertEquals(6.5, result.soilPh)
        assertEquals("FULL_SUN", result.sunExposure)
        assertEquals("GOOD", result.drainage)
        assertEquals(listOf("S", "SW"), result.sunDirections)
        assertEquals("DRIP", result.irrigationType)
        assertEquals("OPEN_FIELD", result.protection)
    }

    @Test
    fun `a flag cannot travel with a replacement value for the same field`() {
        stubBed()

        assertThrows<BadRequestException> {
            service.updateBed(bedId, UpdateBedRequest(clearSoilType = true, soilType = "CLAY"), orgId)
        }
        assertThrows<BadRequestException> {
            service.updateBed(bedId, UpdateBedRequest(clearSoilPh = true, soilPh = 7.0), orgId)
        }
        assertThrows<BadRequestException> {
            service.updateBed(
                bedId,
                UpdateBedRequest(clearSunDirections = true, sunDirections = listOf("N")),
                orgId,
            )
        }
    }

    @Test
    fun `a blank description still clears it, without needing a flag`() {
        stubBed()

        val result = service.updateBed(bedId, UpdateBedRequest(description = ""), orgId)

        assertEquals(null, result.description)
    }

    @Test
    fun `an omitted description is left alone`() {
        stubBed()

        val result = service.updateBed(bedId, UpdateBedRequest(name = "Nytt namn"), orgId)

        assertEquals("Vid muren", result.description)
    }
}

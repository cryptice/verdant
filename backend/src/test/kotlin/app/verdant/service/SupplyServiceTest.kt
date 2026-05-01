package app.verdant.service

import app.verdant.entity.SupplyCategory
import app.verdant.entity.SupplyType
import app.verdant.entity.SupplyUnit
import app.verdant.repository.SupplyInventoryRepository
import app.verdant.repository.SupplyTypeRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SupplyServiceTest {

    private val typeRepo: SupplyTypeRepository = mock()
    private val inventoryRepo: SupplyInventoryRepository = mock()
    private val objectMapper = ObjectMapper()
    private val service = SupplyService(typeRepo, inventoryRepo, objectMapper)

    @Test
    fun `seedInexhaustibleFertilizers persists four FERTILIZER rows for the given org`() {
        whenever(typeRepo.persist(any<SupplyType>())).thenAnswer { it.arguments[0] as SupplyType }

        service.seedInexhaustibleFertilizers(orgId = 42L)

        val captor = argumentCaptor<SupplyType>()
        verify(typeRepo, times(4)).persist(captor.capture())
        val seeded = captor.allValues
        assertEquals(setOf("Hästgödsel", "Hönsgödsel", "Kompost", "Träaska"), seeded.map { it.name }.toSet())
        seeded.forEach { row ->
            assertEquals(42L, row.orgId)
            assertEquals(SupplyCategory.FERTILIZER, row.category)
            assertEquals(SupplyUnit.LITERS, row.unit)
            assertEquals(true, row.inexhaustible)
        }
        val byName = seeded.associateBy { it.name }
        assertEquals("""{"npk":"0.6-0.3-0.5"}""", byName.getValue("Hästgödsel").properties)
        assertEquals("""{"npk":"3.0-2.0-2.0"}""", byName.getValue("Hönsgödsel").properties)
        assertEquals("""{"npk":"1.0-0.5-1.0"}""", byName.getValue("Kompost").properties)
        assertEquals("""{"npk":"0.0-1.0-7.0"}""", byName.getValue("Träaska").properties)
    }
}

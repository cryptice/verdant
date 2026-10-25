package app.verdant.service

import app.verdant.dto.UpdateSeedInventoryRequest
import app.verdant.entity.SeedInventory
import app.verdant.entity.UnitType
import app.verdant.repository.ProviderRepository
import app.verdant.repository.SeedInventoryRepository
import app.verdant.repository.SpeciesProviderRepository
import app.verdant.repository.SpeciesRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SeedInventoryServiceTest {

    private val repo: SeedInventoryRepository = mock()
    private val speciesRepo: SpeciesRepository = mock()
    private val service = SeedInventoryService(repo, speciesRepo, mock<SpeciesProviderRepository>(), mock<ProviderRepository>())

    private val batch = SeedInventory(
        id = 7L, orgId = 10L, speciesId = 100L, quantity = 15,
        costPerUnitCents = 200, unitType = UnitType.SEED, seedsPerPackage = 5,
    )

    private fun updateAndCapture(request: UpdateSeedInventoryRequest): SeedInventory {
        whenever(repo.findById(7L)).thenReturn(batch)
        whenever(speciesRepo.findNamesByIds(any())).thenReturn(mapOf(100L to "Chilipeppar"))
        service.updateInventory(7L, request, orgId = 10L)
        return argumentCaptor<SeedInventory>().also { verify(repo).update(it.capture()) }.firstValue
    }

    @Test
    fun `update saves the edited cost, unit type and package size`() {
        val saved = updateAndCapture(
            UpdateSeedInventoryRequest(costPerUnitCents = 150, unitType = "PLUG", seedsPerPackage = 10),
        )
        assertEquals(150, saved.costPerUnitCents)
        assertEquals(UnitType.PLUG, saved.unitType)
        assertEquals(10, saved.seedsPerPackage)
    }

    @Test
    fun `update keeps fields the request leaves out`() {
        val saved = updateAndCapture(UpdateSeedInventoryRequest(quantity = 12))
        assertEquals(12, saved.quantity)
        assertEquals(200, saved.costPerUnitCents)
        assertEquals(UnitType.SEED, saved.unitType)
        assertEquals(5, saved.seedsPerPackage)
    }
}

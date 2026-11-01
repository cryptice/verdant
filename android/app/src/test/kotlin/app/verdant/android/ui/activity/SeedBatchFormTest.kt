package app.verdant.android.ui.activity

import app.verdant.android.data.model.SpeciesResponse
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class SeedBatchFormTest {
    private fun variety(providers: String, perPackage: Int? = 40, costCents: Int? = 99) = Gson().fromJson(
        """{"id":7,"commonName":"Zinnia","defaultUnitType":"SEED","seedsPerPackage":${perPackage ?: "null"},"costPerSeedCents":${costCents ?: "null"},
            "providers":[$providers],"groups":[],"tags":[],"photos":[],"growingPositions":[],"soils":[]}""",
        SpeciesResponse::class.java,
    )
    private val impecta = """{"id":31,"providerId":1,"providerName":"Impecta","providerIdentifier":"impecta"}"""
    private val florea = """{"id":32,"providerId":2,"providerName":"Florea","providerIdentifier":"florea"}"""

    @Test fun `picking a variety fills in its package size, price and only supplier`() {
        val form = SeedBatchForm().withSpecies(variety(impecta))
        assertEquals("40", form.seedsPerPackage)
        assertEquals("0.99", form.costPerSeed)
        assertEquals("39.6", form.costPerPackage)
        assertEquals(1L, form.providerId)
        assertEquals(40, form.quantity)
    }

    @Test fun `a variety with several suppliers leaves the choice to the grower`() {
        assertNull(SeedBatchForm().withSpecies(variety("$impecta,$florea")).providerId)
        assertNull(SeedBatchForm().withSpecies(variety("")).providerId)
    }

    @Test fun `packages make up the quantity and the request carries supplier, package size and unit price`() {
        val form = SeedBatchForm().withSpecies(variety(impecta, perPackage = null, costCents = null))
            .copy(providerId = 3, packages = "3")
            .withSeedsPerPackage("25")
            .withCostPerPackage("30")
        assertEquals(75, form.quantity)
        assertEquals("1.2", form.costPerSeed)
        val request = form.toRequest()
        assertEquals(75, request.quantity)
        assertEquals(25, request.seedsPerPackage)
        assertEquals(120, request.costPerUnitCents)
        assertEquals(3L, request.providerId)
        assertEquals("SEED", request.unitType)
    }
}

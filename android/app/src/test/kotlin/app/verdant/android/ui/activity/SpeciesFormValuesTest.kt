package app.verdant.android.ui.activity

import org.junit.Assert.*
import org.junit.Test

class SpeciesFormValuesTest {
    @Test fun `only a name is required, and the English name falls back to the Swedish one`() {
        assertFalse(SpeciesFormValues().hasName)
        val form = SpeciesFormValues(commonNameSv = "Zinnia", variantNameSv = "Queen Lime")
        assertTrue(form.hasName)
        val request = form.toCreateRequest(null, null)
        assertEquals("Zinnia", request.commonName)
        assertEquals("Zinnia", request.commonNameSv)
        assertNull(request.variantName)
        assertNull(request.germinationTimeDaysMin)
    }

    @Test fun `copying to English fills only blank English names`() {
        val form = SpeciesFormValues(commonNameSv = "Rosenskära", commonName = "Cosmos", variantNameSv = "Cupcake").copyToEnglish()
        assertEquals("Cosmos", form.commonName)
        assertEquals("Cupcake", form.variantName)
    }

    @Test fun `prices per seed and per package fill each other in through the package size`() {
        var form = SpeciesFormValues().withSeedsPerPackage("40").withCostPerPackage("39,50")
        assertEquals("0.99", form.costPerSeed)
        form = form.withCostPerSeed("1,2")
        assertEquals("48", form.costPerPackage)
        form = form.withSeedsPerPackage("50")
        assertEquals("0.96", form.costPerSeed)
        val request = form.toCreateRequest(null, null)
        assertEquals(96, request.costPerSeedCents)
        assertEquals(50, request.seedsPerPackage)
        assertEquals("", form.withSeedsPerPackage("").costPerPackage)
    }

    @Test fun `the update request carries the commercial figures`() {
        val request = SpeciesFormValues(commonNameSv = "Dahlia", costPerSeed = "4,5", expectedStemsPerPlant = "12", plantType = "TUBER", defaultUnitType = "TUBER")
            .toUpdateRequest(null, null)
        assertEquals(450, request.costPerSeedCents)
        assertEquals(12, request.expectedStemsPerPlant)
        assertEquals("TUBER", request.plantType)
        assertEquals("TUBER", request.defaultUnitType)
    }
}

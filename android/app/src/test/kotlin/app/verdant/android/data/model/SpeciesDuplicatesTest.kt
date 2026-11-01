package app.verdant.android.data.model

import org.junit.Assert.*
import org.junit.Test

class SpeciesDuplicatesTest {
    private fun variety(id: Long, commonNameSv: String, variantNameSv: String? = null) = SpeciesResponse(
        id = id, commonName = commonNameSv, commonNameSv = commonNameSv, variantName = variantNameSv, variantNameSv = variantNameSv,
        scientificName = null, imageFrontUrl = null, imageBackUrl = null, germinationTimeDaysMin = null, germinationTimeDaysMax = null,
        daysToHarvestMin = null, daysToHarvestMax = null, sowingDepthMm = null, growingPositions = emptyList(), soils = emptyList(),
        heightCmMin = null, heightCmMax = null, germinationRate = null, tags = emptyList(), isSystem = true, createdAt = "",
    )

    private val catalogue = listOf(
        variety(1, "Sommardahlia", "Opera White"),
        variety(2, "Pompondahlia", "Lilliput"),
        variety(3, "Zinnia"),
        variety(4, "Zinnia", "Queen Lime"),
    )

    @Test fun `matches the variety name when the species names overlap, ignoring case`() {
        assertEquals(listOf(1L), findDuplicateVarieties(VarietyNames("dahlia", variantNameSv = "opera white"), catalogue).map { it.id })
        assertEquals(listOf(2L), findDuplicateVarieties(VarietyNames("Dahlia", variantName = "Lilliput"), catalogue).map { it.id })
    }

    @Test fun `does not match the same variety name on another species`() {
        assertTrue(findDuplicateVarieties(VarietyNames("Ros", variantNameSv = "Opera White"), catalogue).isEmpty())
    }

    @Test fun `without a variety name, matches only a species without one`() {
        assertEquals(listOf(3L), findDuplicateVarieties(VarietyNames("Zinnia"), catalogue).map { it.id })
    }

    @Test fun `searches by the variety name first`() {
        assertEquals("Lilliput", duplicateSearchText(VarietyNames("Dahlia", variantNameSv = " Lilliput ")))
        assertEquals("Zinnia", duplicateSearchText(VarietyNames("Zinnia")))
    }
}

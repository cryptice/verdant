package app.verdant.resource

import app.verdant.dto.AddSpeciesProviderRequest
import app.verdant.dto.AuthResponse
import app.verdant.dto.CreateSpeciesRequest
import app.verdant.dto.ImportResult
import app.verdant.dto.SpeciesProviderResponse
import app.verdant.dto.SpeciesResponse
import app.verdant.dto.UpdateSpeciesProviderRequest
import app.verdant.dto.UpdateSpeciesRequest
import app.verdant.dto.UserResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * Contract test pinning the field-level shape of DTOs exposed to the
 * Android, admin, and web clients.
 *
 * The four clients hand-type their DTOs against this backend. The
 * cost-per-seed rename — backend used `costPerSeedCents` while admin read
 * `costPerSeedSek` — went unnoticed because nothing tied the typed clients
 * to the live spec. This test fails as soon as a backend DTO drifts, so
 * the client rename has to land in the same change.
 *
 * Maintenance: when adding/removing a field on a covered DTO, update the
 * expected set here AND update every client that consumes it.
 *
 * Property order follows the order Jackson emits via the data class
 * primary constructor.
 */
class ApiContractTest {

    @Test
    fun `SpeciesResponse fields are pinned`() = assertFields(
        SpeciesResponse::class,
        "id", "commonName", "variantName", "commonNameSv", "variantNameSv",
        "scientificName", "imageFrontUrl", "imageBackUrl", "photos",
        "germinationTimeDaysMin", "germinationTimeDaysMax",
        "daysToHarvestMin", "daysToHarvestMax", "sowingDepthMm",
        "growingPositions", "soils", "heightCmMin", "heightCmMax",
        "bloomMonths", "sowingMonths", "germinationRate",
        "groups", "tags", "providers",
        "costPerSeedCents",
        "expectedStemsPerPlant", "expectedVaseLifeDays",
        "plantType", "defaultUnitType",
        "isSystem", "workflowTemplateId", "createdAt",
    )

    @Test
    fun `SpeciesProviderResponse fields are pinned`() = assertFields(
        SpeciesProviderResponse::class,
        "id", "providerId", "providerName", "providerIdentifier",
        "imageFrontUrl", "imageBackUrl", "productUrl",
        "costPerUnitCents", "unitType",
    )

    @Test
    fun `CreateSpeciesRequest fields are pinned`() = assertFields(
        CreateSpeciesRequest::class,
        "commonName", "variantName", "commonNameSv", "variantNameSv",
        "scientificName", "imageFrontBase64", "imageBackBase64",
        "germinationTimeDaysMin", "germinationTimeDaysMax",
        "daysToHarvestMin", "daysToHarvestMax", "sowingDepthMm",
        "growingPositions", "soils", "heightCmMin", "heightCmMax",
        "bloomMonths", "sowingMonths", "germinationRate", "tagIds",
        "costPerSeedCents",
        "expectedStemsPerPlant", "expectedVaseLifeDays",
        "plantType", "defaultUnitType", "workflowTemplateId",
    )

    @Test
    fun `UpdateSpeciesRequest fields are pinned`() = assertFields(
        UpdateSpeciesRequest::class,
        "commonName", "variantName", "commonNameSv", "variantNameSv",
        "scientificName", "imageFrontBase64", "imageBackBase64",
        "germinationTimeDaysMin", "germinationTimeDaysMax",
        "daysToHarvestMin", "daysToHarvestMax", "sowingDepthMm",
        "growingPositions", "soils", "heightCmMin", "heightCmMax",
        "bloomMonths", "sowingMonths", "germinationRate", "tagIds",
        "costPerSeedCents",
        "expectedStemsPerPlant", "expectedVaseLifeDays",
        "plantType", "defaultUnitType", "workflowTemplateId",
        "clearWorkflowTemplate",
    )

    @Test
    fun `AddSpeciesProviderRequest fields are pinned`() = assertFields(
        AddSpeciesProviderRequest::class,
        "providerId", "imageFrontBase64", "imageBackBase64",
        "productUrl", "costPerUnitCents", "unitType",
    )

    @Test
    fun `UpdateSpeciesProviderRequest fields are pinned`() = assertFields(
        UpdateSpeciesProviderRequest::class,
        "imageFrontBase64", "imageBackBase64",
        "productUrl", "costPerUnitCents", "unitType",
    )

    @Test
    fun `UserResponse fields are pinned`() = assertFields(
        UserResponse::class,
        "id", "email", "displayName", "avatarUrl", "role",
        "language", "onboarding", "advancedMode",
        "organizations", "createdAt",
    )

    @Test
    fun `AuthResponse fields are pinned`() = assertFields(
        AuthResponse::class,
        "token", "user",
    )

    @Test
    fun `ImportResult fields are pinned`() = assertFields(
        ImportResult::class,
        "created", "updated", "skipped",
    )

    private fun assertFields(klass: KClass<*>, vararg expected: String) {
        val actual = klass.memberProperties.map { it.name }.toSet()
        assertEquals(
            expected.toSet(),
            actual,
            "${klass.simpleName} fields drifted — clients (android, admin, web) " +
                "will deserialize against the wrong shape until updated.",
        )
    }
}

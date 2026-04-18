package app.verdant.dto

import app.verdant.entity.OrgRole
import app.verdant.entity.Role
import jakarta.validation.constraints.Size
import java.time.Instant

data class UserOrgMembership(
    val orgId: Long,
    val orgName: String,
    val orgEmoji: String?,
    val role: OrgRole,
)

data class UserResponse(
    val id: Long,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: Role,
    val language: String,
    val onboarding: String?,
    val advancedMode: Boolean,
    val organizations: List<UserOrgMembership>,
    val createdAt: Instant
)

data class UpdateUserRequest(
    @field:Size(max = 255)
    val displayName: String? = null,
    @field:Size(max = 255)
    val avatarUrl: String? = null,
    @field:Size(max = 10)
    val language: String? = null,
    val advancedMode: Boolean? = null,
)

data class UpdateOnboardingRequest(
    @field:Size(max = 100)
    val completedSteps: List<String>? = null,
    val dismissed: Boolean? = null,
)

data class UserDataExport(
    val user: UserResponse,
    val gardens: List<GardenResponse>,
    val beds: List<BedResponse>,
    val plants: List<PlantResponse>,
    val plantEvents: List<PlantEventResponse>,
    val species: List<SpeciesResponse>,
    val seedInventory: List<SeedInventoryResponse>,
    val scheduledTasks: List<ScheduledTaskResponse>,
    val seasons: List<SeasonResponse>,
    val customers: List<CustomerResponse>,
    val pestDiseaseLogs: List<PestDiseaseLogResponse>,
    val varietyTrials: List<VarietyTrialResponse>,
    val bouquetRecipes: List<BouquetRecipeResponse>,
    val successionSchedules: List<SuccessionScheduleResponse>,
    val productionTargets: List<ProductionTargetResponse>,
    val exportedAt: Instant,
)

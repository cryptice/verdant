package app.verdant.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class TrayLocationResponse(
    val id: Long,
    val name: String,
    val activePlantCount: Int,
    val createdAt: Instant,
)

data class CreateTrayLocationRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
)

data class UpdateTrayLocationRequest(
    @field:Size(max = 255)
    val name: String? = null,
)

data class BulkLocationActionResponse(val plantsAffected: Int)

data class BulkLocationNoteRequest(
    @field:NotBlank @field:Size(max = 2000)
    val text: String,
)

/** [count] = -1 means "all matching". [targetLocationId] = null means "detach". */
data class MoveTrayLocationRequest(
    val targetLocationId: Long? = null,
    val count: Int = -1,
    val speciesId: Long? = null,
    val status: String? = null,
)

/** Move tray plants between locations, including the unassigned (null) case
 *  on either side. [fromTrayLocationId] = null targets plants currently in
 *  trays without a location; [toTrayLocationId] = null detaches them. */
data class MoveTrayPlantsRequest(
    val fromTrayLocationId: Long? = null,
    val toTrayLocationId: Long? = null,
    @field:jakarta.validation.constraints.NotNull
    val speciesId: Long,
    @field:jakarta.validation.constraints.NotBlank
    val status: String,
    @field:jakarta.validation.constraints.Min(1)
    val count: Int,
)

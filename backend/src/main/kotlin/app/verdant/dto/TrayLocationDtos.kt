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

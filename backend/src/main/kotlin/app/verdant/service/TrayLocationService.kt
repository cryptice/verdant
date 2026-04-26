package app.verdant.service

import app.verdant.dto.CreateTrayLocationRequest
import app.verdant.dto.TrayLocationResponse
import app.verdant.dto.UpdateTrayLocationRequest
import app.verdant.entity.TrayLocation
import app.verdant.repository.TrayLocationRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class TrayLocationService(private val repo: TrayLocationRepository) {

    fun list(orgId: Long): List<TrayLocationResponse> =
        repo.findByOrgId(orgId).map { it.toResponse() }

    @Transactional
    fun create(request: CreateTrayLocationRequest, orgId: Long): TrayLocationResponse =
        repo.persist(TrayLocation(orgId = orgId, name = request.name.trim())).toResponse()

    @Transactional
    fun update(id: Long, request: UpdateTrayLocationRequest, orgId: Long): TrayLocationResponse {
        val loc = repo.findById(id) ?: throw NotFoundException("Tray location not found")
        if (loc.orgId != orgId) throw NotFoundException("Tray location not found")
        val updated = loc.copy(
            name = request.name?.trim()?.takeIf { it.isNotBlank() } ?: loc.name,
        )
        repo.update(updated)
        return updated.toResponse()
    }

    /** Slice 1: simple delete. Slice 3 will emit MOVED audit events first. */
    @Transactional
    fun delete(id: Long, orgId: Long) {
        val loc = repo.findById(id) ?: throw NotFoundException("Tray location not found")
        if (loc.orgId != orgId) throw NotFoundException("Tray location not found")
        repo.delete(id)
    }

    private fun TrayLocation.toResponse() = TrayLocationResponse(
        id = id!!,
        name = name,
        activePlantCount = repo.countActivePlants(id),
        createdAt = createdAt,
    )
}

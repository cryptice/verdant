package app.verdant.service

import app.verdant.dto.CreateOutletRequest
import app.verdant.dto.OutletResponse
import app.verdant.dto.UpdateOutletRequest
import app.verdant.entity.Outlet
import app.verdant.repository.OutletRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException

@ApplicationScoped
class OutletService(
    private val repo: OutletRepository,
) {
    fun list(orgId: Long): List<OutletResponse> =
        repo.findByOrgId(orgId).map { it.toResponse() }

    fun get(id: Long, orgId: Long): OutletResponse {
        val outlet = repo.findById(id) ?: throw NotFoundException("Outlet not found")
        if (outlet.orgId != orgId) throw NotFoundException("Outlet not found")
        return outlet.toResponse()
    }

    fun create(request: CreateOutletRequest, orgId: Long): OutletResponse =
        repo.persist(
            Outlet(
                orgId = orgId,
                name = request.name,
                channel = request.channel,
                contactInfo = request.contactInfo,
                notes = request.notes,
            ),
        ).toResponse()

    fun update(id: Long, request: UpdateOutletRequest, orgId: Long): OutletResponse {
        val outlet = repo.findById(id) ?: throw NotFoundException("Outlet not found")
        if (outlet.orgId != orgId) throw NotFoundException("Outlet not found")
        val updated = outlet.copy(
            name = request.name ?: outlet.name,
            channel = request.channel ?: outlet.channel,
            contactInfo = request.contactInfo ?: outlet.contactInfo,
            notes = request.notes ?: outlet.notes,
        )
        repo.update(updated)
        return updated.toResponse()
    }

    /**
     * Delete is RESTRICT-guarded at the DB level: outlet rows referenced by any
     * sale_lot.current_outlet_id or sale.outlet_id can't be removed. A DB
     * exception there bubbles up as 409 so the user sees a meaningful error.
     */
    fun delete(id: Long, orgId: Long) {
        val outlet = repo.findById(id) ?: throw NotFoundException("Outlet not found")
        if (outlet.orgId != orgId) throw NotFoundException("Outlet not found")
        try {
            repo.delete(id)
        } catch (e: Exception) {
            // Postgres FK violation surfaces as a SQL state '23503'. Translate to 409.
            val msg = e.message ?: ""
            if (msg.contains("foreign key", ignoreCase = true) || msg.contains("23503")) {
                throw WebApplicationException(
                    "Outlet is referenced by existing sales or active lots and cannot be deleted",
                    409,
                )
            }
            throw e
        }
    }

    private fun Outlet.toResponse() = OutletResponse(
        id = id!!,
        name = name,
        channel = channel,
        contactInfo = contactInfo,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

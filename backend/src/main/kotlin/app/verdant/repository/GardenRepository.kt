package app.verdant.repository

import app.verdant.entity.Garden
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class GardenRepository : PanacheRepository<Garden> {
    fun findByOwnerId(ownerId: Long): List<Garden> = list("owner.id", ownerId)
}

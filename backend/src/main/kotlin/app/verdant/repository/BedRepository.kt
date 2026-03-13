package app.verdant.repository

import app.verdant.entity.Bed
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class BedRepository : PanacheRepository<Bed> {
    fun findByGardenId(gardenId: Long): List<Bed> = list("garden.id", gardenId)
}

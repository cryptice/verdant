package app.verdant.repository

import app.verdant.entity.Plant
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class PlantRepository : PanacheRepository<Plant> {
    fun findByBedId(bedId: Long): List<Plant> = list("bed.id", bedId)
    fun countByBedGardenId(gardenId: Long): Long = count("bed.garden.id", gardenId)
    fun countByBedGardenOwnerId(ownerId: Long): Long = count("bed.garden.owner.id", ownerId)
}

package app.verdant.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate

@Entity
class Plant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    lateinit var name: String

    var species: String? = null
    var plantedDate: LocalDate? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PlantStatus = PlantStatus.SEEDLING

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bed_id", nullable = false)
    lateinit var bed: Bed

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
    var updatedAt: Instant = Instant.now()

    @PreUpdate
    fun onUpdate() { updatedAt = Instant.now() }
}

enum class PlantStatus { SEEDLING, GROWING, MATURE, HARVESTED, REMOVED }

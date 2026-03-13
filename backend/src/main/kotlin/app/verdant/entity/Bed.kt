package app.verdant.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
class Bed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    lateinit var name: String

    var description: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garden_id", nullable = false)
    lateinit var garden: Garden

    @OneToMany(mappedBy = "bed", cascade = [CascadeType.ALL], orphanRemoval = true)
    var plants: MutableList<Plant> = mutableListOf()

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
    var updatedAt: Instant = Instant.now()

    @PreUpdate
    fun onUpdate() { updatedAt = Instant.now() }
}

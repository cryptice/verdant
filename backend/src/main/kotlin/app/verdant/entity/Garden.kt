package app.verdant.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
class Garden {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    lateinit var name: String

    var description: String? = null
    var emoji: String? = "\uD83C\uDF31"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    lateinit var owner: User

    @OneToMany(mappedBy = "garden", cascade = [CascadeType.ALL], orphanRemoval = true)
    var beds: MutableList<Bed> = mutableListOf()

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
    var updatedAt: Instant = Instant.now()

    @PreUpdate
    fun onUpdate() { updatedAt = Instant.now() }
}

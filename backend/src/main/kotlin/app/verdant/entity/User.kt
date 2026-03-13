package app.verdant.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "app_user")
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false, unique = true)
    lateinit var googleSubject: String

    @Column(nullable = false)
    lateinit var email: String

    lateinit var displayName: String

    var avatarUrl: String? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role = Role.USER

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    var updatedAt: Instant = Instant.now()

    @OneToMany(mappedBy = "owner", cascade = [CascadeType.ALL], orphanRemoval = true)
    var gardens: MutableList<Garden> = mutableListOf()

    @PreUpdate
    fun onUpdate() { updatedAt = Instant.now() }
}

enum class Role { USER, ADMIN }

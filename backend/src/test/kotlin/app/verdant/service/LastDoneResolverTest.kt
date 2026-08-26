package app.verdant.service

import app.verdant.entity.*
import app.verdant.repository.*
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

@QuarkusTest
class LastDoneResolverTest {

    @Inject lateinit var resolver: LastDoneResolver
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var gardens: GardenRepository
    @Inject lateinit var beds: BedRepository
    @Inject lateinit var bedEvents: BedEventRepository
    @Inject lateinit var areas: GardenAreaRepository
    @Inject lateinit var areaEvents: GardenAreaEventRepository
    @Inject lateinit var users: UserRepository
    @Inject lateinit var supplyTypes: SupplyTypeRepository
    @Inject lateinit var supplyInventories: SupplyInventoryRepository
    @Inject lateinit var supplyApplications: SupplyApplicationRepository

    private var orgId: Long = 0
    private var bedId: Long = 0
    private var areaId: Long = 0

    @BeforeEach
    fun setUp() {
        orgId = orgs.persist(Organization(name = "Resolver org")).id!!
        val gardenId = gardens.persist(Garden(name = "Resolver garden", orgId = orgId)).id!!
        bedId = beds.persist(Bed(name = "Bädd 1", gardenId = gardenId)).id!!
        areaId = areas.persist(
            GardenArea(gardenId = gardenId, name = "Gång", category = GardenAreaCategory.WALKWAY)
        ).id!!
    }

    private fun bedRule(activity: MaintenanceActivity) =
        MaintenanceRule(orgId = orgId, bedId = bedId, activity = activity, intervalDays = 14)

    private fun areaRule(activity: MaintenanceActivity) =
        MaintenanceRule(orgId = orgId, gardenAreaId = areaId, activity = activity, intervalDays = 14)

    @Test
    fun `a bed never weeded resolves to null`() {
        assertNull(resolver.resolve(bedRule(MaintenanceActivity.WEED)))
    }

    @Test
    fun `bed weeding resolves to the newest WEEDED bed event`() {
        bedEvents.persist(BedEvent(bedId = bedId, eventType = PlantEventType.WEEDED, eventDate = LocalDate.of(2026, 5, 1)))
        bedEvents.persist(BedEvent(bedId = bedId, eventType = PlantEventType.WEEDED, eventDate = LocalDate.of(2026, 6, 1)))

        assertEquals(LocalDate.of(2026, 6, 1), resolver.resolve(bedRule(MaintenanceActivity.WEED)))
    }

    @Test
    fun `watering does not satisfy a weeding rule`() {
        bedEvents.persist(BedEvent(bedId = bedId, eventType = PlantEventType.WATERED, eventDate = LocalDate.of(2026, 6, 1)))
        assertNull(resolver.resolve(bedRule(MaintenanceActivity.WEED)))
    }

    @Test
    fun `area weeding resolves to the newest matching area event`() {
        areaEvents.persist(GardenAreaEvent(gardenAreaId = areaId, eventType = "WEED", eventDate = LocalDate.of(2026, 6, 10)))
        areaEvents.persist(GardenAreaEvent(gardenAreaId = areaId, eventType = "MOW", eventDate = LocalDate.of(2026, 7, 1)))

        assertEquals(LocalDate.of(2026, 6, 10), resolver.resolve(areaRule(MaintenanceActivity.WEED)))
        assertEquals(LocalDate.of(2026, 7, 1), resolver.resolve(areaRule(MaintenanceActivity.MOW)))
    }

    @Test
    fun `fertilizing resolves from an APPLIED_SUPPLY bed event`() {
        bedEvents.persist(
            BedEvent(bedId = bedId, eventType = PlantEventType.APPLIED_SUPPLY, eventDate = LocalDate.of(2026, 5, 20))
        )
        assertEquals(LocalDate.of(2026, 5, 20), resolver.resolve(bedRule(MaintenanceActivity.FERTILIZE)))
    }

    @Test
    fun `fertilizing resolves from a supply application`() {
        val userId = users.persist(User(email = "resolver@test.com", displayName = "Resolver")).id!!
        val supplyTypeId = supplyTypes.persist(
            SupplyType(
                orgId = orgId, name = "Hönsgödsel",
                category = SupplyCategory.FERTILIZER, unit = SupplyUnit.KILOGRAMS,
            )
        ).id!!
        val inventoryId = supplyInventories.persist(
            SupplyInventory(orgId = orgId, supplyTypeId = supplyTypeId, quantity = BigDecimal("100.00"))
        ).id!!

        supplyApplications.insert(
            SupplyApplication(
                orgId = orgId,
                bedId = bedId,
                supplyInventoryId = inventoryId,
                supplyTypeId = supplyTypeId,
                quantity = BigDecimal("5.00"),
                targetScope = SupplyApplicationScope.BED,
                appliedAt = LocalDate.of(2026, 5, 20).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                appliedBy = userId,
            )
        )

        // No bed_event row at all — the clock has to move on the application alone.
        assertEquals(LocalDate.of(2026, 5, 20), resolver.resolve(bedRule(MaintenanceActivity.FERTILIZE)))
    }
}

package app.verdant.service

import app.verdant.dto.ReviewStep
import app.verdant.dto.SeedReviewResponse
import app.verdant.dto.VarietyReview
import app.verdant.entity.MovementKind
import app.verdant.entity.OrgMember
import app.verdant.entity.Organization
import app.verdant.entity.Role
import app.verdant.entity.Season
import app.verdant.entity.SeedInventory
import app.verdant.entity.SeedStockMovement
import app.verdant.entity.Species
import app.verdant.entity.SpeciesWorkflowStep
import app.verdant.entity.UnitType
import app.verdant.entity.User
import app.verdant.repository.OrgMemberRepository
import app.verdant.repository.OrganizationRepository
import app.verdant.repository.SeasonRepository
import app.verdant.repository.SeedInventoryRepository
import app.verdant.repository.SeedStockMovementRepository
import app.verdant.repository.SpeciesRepository
import app.verdant.repository.UserRepository
import app.verdant.repository.WorkflowRepository
import app.verdant.resource.PlanningApiProfile
import com.fasterxml.jackson.databind.ObjectMapper
import io.agroal.api.AgroalDataSource
import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.smallrye.jwt.build.Jwt
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.sql.Statement
import java.time.LocalDate
import java.util.Base64
import java.util.Optional
import java.util.UUID

@QuarkusTest
@TestProfile(PlanningApiProfile::class)
class SeedReviewServiceTest {

    @Inject lateinit var service: SeedReviewService
    @Inject lateinit var ds: AgroalDataSource
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var users: UserRepository
    @Inject lateinit var members: OrgMemberRepository
    @Inject lateinit var seasons: SeasonRepository
    @Inject lateinit var speciesRepo: SpeciesRepository
    @Inject lateinit var workflows: WorkflowRepository
    @Inject lateinit var inventory: SeedInventoryRepository
    @Inject lateinit var movements: SeedStockMovementRepository
    @Inject lateinit var mapper: ObjectMapper
    @TestHTTPResource lateinit var base: URI
    @ConfigProperty(name = "smallrye.jwt.sign.key") lateinit var signingKey: Optional<String>

    private val http = HttpClient.newHttpClient()

    // Sales and lots restrict deleting their outlet, plant and user, which would block other test classes that wipe
    // organizations from the shared (reused) test database. Remove ours, including any left by earlier runs.
    @AfterEach
    fun removeSales() {
        ds.connection.use { conn ->
            val ours = "SELECT id FROM organization WHERE name = 'Seed review org'"
            conn.prepareStatement("DELETE FROM sale WHERE sale_lot_id IN (SELECT id FROM sale_lot WHERE org_id IN ($ours))").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM sale_lot WHERE org_id IN ($ours)").use { it.executeUpdate() }
        }
    }

    private class Fixture(
        val orgId: Long,
        val userId: Long,
        val outletId: Long,
        val season: Season,
        val a: Long,
        val b: Long,
        val aPlants: List<Long>,
    )

    /**
     * Season 2026-03-01..2026-10-31. Species A follows a workflow
     * (Sådd → Krukning → Utplantering → Skörd, plus a side branch and a step
     * without an event type); species B has none. A: 10 sown, 8 potted,
     * 7 planted out, 6 harvested (34 stems), 11 stems sold for 10 500 öre,
     * 4 seeds left in two batches costing 20 and 40 öre. B: 3 sown,
     * 2 harvested (8 stems).
     */
    private fun fixture(): Fixture {
        val orgId = orgs.persist(Organization(name = "Seed review org")).id!!
        val userId = users.persist(User(email = "${UUID.randomUUID()}@example.test", displayName = "Seed review", role = Role.USER)).id!!
        members.persist(OrgMember(orgId = orgId, userId = userId))
        val outletId = insert("INSERT INTO outlet (org_id, name, channel) VALUES (?, ?, 'FLORIST')", orgId, "Outlet ${UUID.randomUUID()}")
        val season = seasons.persist(
            Season(orgId = orgId, name = "Seed review 2026", year = 2026,
                startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 10, 31))
        )
        val a = speciesRepo.persist(
            Species(orgId = orgId, commonName = "Cosmos", commonNameSv = "Rosenskära", variantName = "Purity")
        ).id!!
        listOf(
            SpeciesWorkflowStep(speciesId = a, name = "Sådd", eventType = "SEEDED", sortOrder = 1),
            SpeciesWorkflowStep(speciesId = a, name = "Krukning", eventType = "POTTED_UP", sortOrder = 2),
            SpeciesWorkflowStep(speciesId = a, name = "Knipsning", eventType = "PINCHED", isSideBranch = true, sortOrder = 3),
            SpeciesWorkflowStep(speciesId = a, name = "Utplantering", eventType = "PLANTED_OUT", sortOrder = 4),
            SpeciesWorkflowStep(speciesId = a, name = "Gödsling", eventType = null, sortOrder = 5),
            SpeciesWorkflowStep(speciesId = a, name = "Skörd", eventType = "HARVESTED", sortOrder = 6),
        ).forEach { workflows.persistSpeciesStep(it) }
        val b = speciesRepo.persist(Species(orgId = orgId, commonName = "Zinnia", defaultUnitType = UnitType.TUBER)).id!!

        val aPlants = (1..10).map { plant(orgId, a, LocalDate.of(2026, 4, 1)) }
        aPlants.forEach { event(it, "SEEDED", LocalDate.of(2026, 4, 1)) }
        aPlants.take(8).forEach { event(it, "POTTED_UP", LocalDate.of(2026, 4, 20)) }
        aPlants.take(7).forEach { event(it, "PLANTED_OUT", LocalDate.of(2026, 5, 15)) }
        val harvests = aPlants.take(6).map { event(it, "HARVESTED", LocalDate.of(2026, 7, 1), stemCount = 5) }
        event(aPlants[0], "HARVESTED", LocalDate.of(2026, 8, 1), stemCount = 4)
        aPlants.take(3).forEach { event(it, "PINCHED", LocalDate.of(2026, 5, 1)) }

        val bPlants = (1..3).map { plant(orgId, b, LocalDate.of(2026, 4, 15)) }
        bPlants.forEach { event(it, "SEEDED", LocalDate.of(2026, 4, 15)) }
        bPlants.take(2).forEach { event(it, "HARVESTED", LocalDate.of(2026, 8, 10), quantity = 4) }

        inventory.persist(SeedInventory(orgId = orgId, speciesId = a, quantity = 3, costPerUnitCents = 20))
        inventory.persist(SeedInventory(orgId = orgId, speciesId = a, quantity = 1, costPerUnitCents = 40))

        val bunchLot = insert(
            """INSERT INTO sale_lot (org_id, source_kind, harvest_event_id, unit_kind, stems_per_unit, quantity_total, quantity_remaining,
                   initial_requested_price_cents, current_requested_price_cents, current_outlet_id)
               VALUES (?, 'HARVEST_EVENT', ?, 'BUNCH', 5, 10, 7, 4000, 4000, ?)""",
            orgId, harvests[0], outletId,
        )
        sale(bunchLot, 2, 4000, LocalDate.of(2026, 7, 2), outletId, userId)
        sale(bunchLot, 1, 4000, LocalDate.of(2026, 11, 15), outletId, userId)
        val plugLot = insert(
            """INSERT INTO sale_lot (org_id, source_kind, plant_id, unit_kind, quantity_total, quantity_remaining,
                   initial_requested_price_cents, current_requested_price_cents, current_outlet_id)
               VALUES (?, 'PLANT', ?, 'PLUG', 1, 0, 2500, 2500, ?)""",
            orgId, aPlants[9], outletId,
        )
        sale(plugLot, 1, 2500, LocalDate.of(2026, 5, 1), outletId, userId)

        return Fixture(orgId, userId, outletId, season, a, b, aPlants)
    }

    private fun review(f: Fixture, season: Season = f.season) = service.review(season.id!!, f.orgId)

    private fun SeedReviewResponse.of(speciesId: Long): VarietyReview = varieties.single { it.speciesId == speciesId }

    @Test
    fun `counts each workflow step from events in the season`() {
        val f = fixture()

        val r = review(f)

        assertEquals(listOf(f.a, f.b), r.varieties.map { it.speciesId })
        val a = r.of(f.a)
        assertEquals("Rosenskära – Purity", a.speciesName)
        assertEquals("SEED", a.unitType)
        assertEquals(
            listOf(
                ReviewStep("Sådd", "SEEDED", 10),
                ReviewStep("Krukning", "POTTED_UP", 8),
                ReviewStep("Utplantering", "PLANTED_OUT", 7),
                ReviewStep("Skörd", "HARVESTED", 6),
            ),
            a.steps,
        )
        assertEquals(10, a.sown)
        assertEquals(34, a.harvestedStems)
        assertEquals(11, a.soldStems)
        assertEquals(10_500L, a.revenueCents)
        assertEquals(4, a.leftInStock)

        val b = r.of(f.b)
        assertEquals("Zinnia", b.speciesName)
        assertEquals("TUBER", b.unitType)
        assertEquals(listOf(ReviewStep("Sått", "SEEDED", 3), ReviewStep("Skördat", "HARVESTED", 2)), b.steps)
        assertEquals(3, b.sown)
        assertEquals(8, b.harvestedStems)
        assertEquals(0, b.soldStems)
        assertEquals(0L, b.revenueCents)
        assertEquals(0, b.leftInStock)
    }

    @Test
    fun `harvest and sales count by date, not by sowing season`() {
        val f = fixture()
        val old = plant(f.orgId, f.a, LocalDate.of(2025, 10, 1))
        event(old, "SEEDED", LocalDate.of(2025, 10, 1))
        event(old, "HARVESTED", LocalDate.of(2026, 5, 1), stemCount = 7)
        val oldLot = insert(
            """INSERT INTO sale_lot (org_id, source_kind, plant_id, unit_kind, quantity_total, quantity_remaining,
                   initial_requested_price_cents, current_requested_price_cents, current_outlet_id)
               VALUES (?, 'PLANT', ?, 'TUBER', 3, 0, 1000, 1000, ?)""",
            f.orgId, old, f.outletId,
        )
        sale(oldLot, 3, 1000, LocalDate.of(2026, 6, 1), f.outletId, f.userId)

        val a = review(f).of(f.a)

        assertEquals(10, a.sown)
        assertEquals(41, a.harvestedStems)
        assertEquals(14, a.soldStems)
        assertEquals(13_500L, a.revenueCents)
        assertEquals(10, a.steps.single { it.eventType == "SEEDED" }.count)
        assertEquals(7, a.steps.single { it.eventType == "HARVESTED" }.count)
    }

    @Test
    fun `bouquet lots are left out of sales`() {
        val f = fixture()
        val before = review(f)

        val bouquet = insert("INSERT INTO bouquet (org_id, name) VALUES (?, 'Seed review bouquet')", f.orgId)
        val lot = insert(
            """INSERT INTO sale_lot (org_id, source_kind, bouquet_id, unit_kind, quantity_total, quantity_remaining,
                   initial_requested_price_cents, current_requested_price_cents, current_outlet_id)
               VALUES (?, 'BOUQUET', ?, 'BOUQUET', 1, 0, 30000, 30000, ?)""",
            f.orgId, bouquet, f.outletId,
        )
        sale(lot, 1, 30000, LocalDate.of(2026, 7, 1), f.outletId, f.userId)

        assertEquals(before.varieties, review(f).varieties)
    }

    @Test
    fun `registered plants are not counted as sown`() {
        val f = fixture()
        repeat(5) { plant(f.orgId, f.a, LocalDate.of(2026, 4, 1), origin = "REGISTERED") }

        val a = review(f).of(f.a)

        assertEquals(10, a.sown)
        assertEquals(6, a.suggestedOrder)
    }

    @Test
    fun `a season that started before the ledger is estimated`() {
        val f = fixture()
        movement(f, f.a, MovementKind.ADDED, 100, unitCost = 20, seasonId = f.season.id)
        val stockOnly = speciesRepo.persist(Species(orgId = f.orgId, commonName = "Seed review stock only")).id!!
        inventory.persist(SeedInventory(orgId = f.orgId, speciesId = stockOnly, quantity = 50, costPerUnitCents = 10))

        val r = review(f)

        assertTrue(r.estimated)
        assertEquals(f.season.id, r.season.id)
        assertEquals("Seed review 2026", r.season.name)
        assertEquals(2026, r.season.year)
        assertEquals(LocalDate.of(2026, 3, 1), r.season.startDate)
        assertEquals(LocalDate.of(2026, 10, 31), r.season.endDate)
        val a = r.of(f.a)
        assertEquals(a.leftInStock + a.sown, a.bought)
        assertEquals(14, a.bought)
        assertEquals(300L, a.seedCostCents)
        val b = r.of(f.b)
        assertEquals(3, b.bought)
        assertEquals(0L, b.seedCostCents)
        assertEquals(listOf(f.a, f.b), r.varieties.map { it.speciesId })
    }

    @Test
    fun `a season that started after the ledger uses the recorded movements`() {
        val f = fixture()
        val year = movements.ledgerStartDate()!!.year + 1
        val exact = seasons.persist(
            Season(orgId = f.orgId, name = "Seed review $year", year = year,
                startDate = LocalDate.of(year, 1, 1), endDate = LocalDate.of(year, 12, 31))
        )
        repeat(12) { plant(f.orgId, f.a, LocalDate.of(year, 3, 1)) }
        val inYear = LocalDate.of(year, 5, 1)
        val outside = LocalDate.of(2026, 6, 1)
        movement(f, f.a, MovementKind.ADDED, 50, unitCost = 20, seasonId = exact.id, occurredOn = inYear)
        movement(f, f.a, MovementKind.ADDED, 20, unitCost = 40, seasonId = exact.id, occurredOn = inYear)
        movement(f, f.a, MovementKind.ADDED, 30, unitCost = 20, seasonId = f.season.id, occurredOn = outside)
        movement(f, f.a, MovementKind.OPENING, 100, unitCost = 20, seasonId = exact.id, occurredOn = inYear)
        movement(f, f.a, MovementKind.ADJUSTED, -5, unitCost = 20, seasonId = exact.id, occurredOn = inYear)
        movement(f, f.a, MovementKind.SOWN, 12, unitCost = 25, seasonId = exact.id, occurredOn = inYear)
        movement(f, f.a, MovementKind.SOWN, 3, unitCost = null, seasonId = exact.id, occurredOn = inYear)
        movement(f, f.a, MovementKind.SOWN, 9, unitCost = 99, seasonId = f.season.id, occurredOn = outside)
        val boughtOnly = speciesRepo.persist(Species(orgId = f.orgId, commonName = "Seed review bought only")).id!!
        movement(f, boughtOnly, MovementKind.ADDED, 40, unitCost = 10, seasonId = exact.id, occurredOn = inYear)

        val r = review(f, exact)

        assertFalse(r.estimated)
        val a = r.of(f.a)
        assertEquals(12, a.sown)
        assertEquals(70, a.bought)
        assertEquals(300L, a.seedCostCents)
        assertEquals(0, a.harvestedStems)
        assertEquals(8, a.suggestedOrder)
        val c = r.of(boughtOnly)
        assertEquals(40, c.bought)
        assertEquals(0, c.sown)
        assertEquals(0, c.suggestedOrder)
        assertTrue(r.varieties.none { it.speciesId == f.b })
    }

    @Test
    fun `movements count by date even without a stored season`() {
        val f = fixture()
        val year = movements.ledgerStartDate()!!.year + 1
        val exact = seasons.persist(
            Season(orgId = f.orgId, name = "Seed review $year", year = year,
                startDate = LocalDate.of(year, 1, 1), endDate = LocalDate.of(year, 12, 31))
        )
        repeat(5) { plant(f.orgId, f.a, LocalDate.of(year, 3, 1)) }
        movement(f, f.a, MovementKind.ADDED, 25, unitCost = 10, seasonId = null, occurredOn = LocalDate.of(year, 2, 1))
        movement(f, f.a, MovementKind.SOWN, 5, unitCost = 30, seasonId = null, occurredOn = LocalDate.of(year, 3, 1))
        movement(f, f.a, MovementKind.ADDED, 99, unitCost = 10, seasonId = exact.id, occurredOn = LocalDate.of(2026, 6, 1))

        val a = review(f, exact).of(f.a)

        assertEquals(25, a.bought)
        assertEquals(150L, a.seedCostCents)
    }

    @Test
    fun `registered plants do not count in the seeded step`() {
        val f = fixture()
        repeat(5) {
            val p = plant(f.orgId, f.a, LocalDate.of(2026, 4, 1), origin = "REGISTERED")
            event(p, "SEEDED", LocalDate.of(2026, 4, 1))
        }

        val a = review(f).of(f.a)

        assertEquals(10, a.steps.single { it.eventType == "SEEDED" }.count)
    }

    @Test
    fun `suggests sown minus stock and keeps a saved order`() {
        val f = fixture()
        val seasonId = f.season.id!!

        val first = review(f).of(f.a)
        assertEquals(6, first.suggestedOrder)
        assertNull(first.savedOrder)

        val saved = request(f, "PUT", "/api/seed-review/$seasonId/order-list",
            mapOf("lines" to listOf(mapOf("speciesId" to f.a, "quantity" to 12))))
        assertEquals(200, saved.statusCode(), saved.body())
        val body = mapper.readValue(saved.body(), SeedReviewResponse::class.java)
        assertEquals(12, body.of(f.a).savedOrder)
        assertNull(body.of(f.b).savedOrder)
        assertEquals(12, review(f).of(f.a).savedOrder)
        assertEquals(6, review(f).of(f.a).suggestedOrder)

        val negative = request(f, "PUT", "/api/seed-review/$seasonId/order-list",
            mapOf("lines" to listOf(mapOf("speciesId" to f.a, "quantity" to -1))))
        assertEquals(400, negative.statusCode(), negative.body())
        assertEquals(12, review(f).of(f.a).savedOrder)

        val replaced = request(f, "PUT", "/api/seed-review/$seasonId/order-list",
            mapOf("lines" to listOf(mapOf("speciesId" to f.b, "quantity" to 2))))
        assertEquals(200, replaced.statusCode(), replaced.body())
        assertNull(review(f).of(f.a).savedOrder)
        assertEquals(2, review(f).of(f.b).savedOrder)

        val fetched = request(f, "GET", "/api/seed-review?seasonId=$seasonId")
        assertEquals(200, fetched.statusCode(), fetched.body())
        val json = mapper.readTree(fetched.body())
        assertEquals(seasonId, json["season"]["id"].asLong())
        assertTrue(json["estimated"].asBoolean())
        assertEquals(f.a, json["varieties"][0]["speciesId"].asLong())
        assertEquals("SEEDED", json["varieties"][0]["steps"][0]["eventType"].asText())
        assertEquals("2026-03-01", json["season"]["startDate"].asText())
    }

    @Test
    fun `another organization's season is not found`() {
        val f = fixture()
        val other = fixture()

        assertThrows<NotFoundException> { service.review(other.season.id!!, f.orgId) }
        val put = request(f, "PUT", "/api/seed-review/${other.season.id}/order-list",
            mapOf("lines" to listOf(mapOf("speciesId" to f.a, "quantity" to 1))))
        assertEquals(404, put.statusCode(), put.body())
        val foreignSpecies = request(f, "PUT", "/api/seed-review/${f.season.id}/order-list",
            mapOf("lines" to listOf(mapOf("speciesId" to other.a, "quantity" to 1))))
        assertEquals(400, foreignSpecies.statusCode(), foreignSpecies.body())
    }

    // ── fixture helpers ──

    private fun request(f: Fixture, method: String, path: String, body: Any? = null): HttpResponse<String> {
        val key = KeyFactory.getInstance("RSA").generatePrivate(
            PKCS8EncodedKeySpec(Base64.getDecoder().decode(signingKey.orElseThrow().replace(Regex("-----[^-]+-----|\\s"), "")))
        )
        val token = Jwt.issuer("verdant-api").subject(f.userId.toString()).claim("userId", f.userId).groups(setOf("USER")).sign(key)
        val publisher = if (body == null) HttpRequest.BodyPublishers.noBody() else HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))
        val req = HttpRequest.newBuilder(base.resolve(path))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $token")
            .header("X-Organization-Id", f.orgId.toString())
            .method(method, publisher)
            .build()
        return http.send(req, HttpResponse.BodyHandlers.ofString())
    }

    private fun plant(orgId: Long, speciesId: Long, planted: LocalDate, origin: String = "SOWN") =
        insert(
            "INSERT INTO plant (name, species_id, planted_date, status, seed_count, surviving_count, org_id, origin) VALUES ('Review plant', ?, ?, 'SEEDED', 1, 1, ?, ?)",
            speciesId, planted, orgId, origin,
        )

    private fun event(plantId: Long, type: String, date: LocalDate, stemCount: Int? = null, quantity: Int? = null) =
        insert(
            "INSERT INTO plant_event (plant_id, event_type, event_date, stem_count, quantity) VALUES (?, ?, ?, ?, ?)",
            plantId, type, date, stemCount, quantity,
        )

    private fun sale(lotId: Long, quantity: Int, price: Int, soldAt: LocalDate, outletId: Long, userId: Long) =
        insert(
            "INSERT INTO sale (sale_lot_id, quantity, price_per_unit_cents, outlet_id, sold_at, recorded_by_user_id) VALUES (?, ?, ?, ?, ?, ?)",
            lotId, quantity, price, outletId, soldAt, userId,
        )

    private fun movement(
        f: Fixture, speciesId: Long, kind: MovementKind, quantity: Int, unitCost: Int?, seasonId: Long?,
        occurredOn: LocalDate = LocalDate.now(),
    ) =
        movements.persist(
            SeedStockMovement(
                orgId = f.orgId, seedInventoryId = null, speciesId = speciesId, kind = kind, quantity = quantity,
                unitCostCents = unitCost, occurredOn = occurredOn, seasonId = seasonId,
            )
        )

    private fun insert(sql: String, vararg params: Any?): Long =
        ds.connection.use { conn ->
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { ps ->
                params.forEachIndexed { i, p -> ps.setObject(i + 1, p) }
                ps.executeUpdate()
                ps.generatedKeys.use { rs -> rs.next(); rs.getLong(1) }
            }
        }
}

package app.verdant.android.ui.maintenance

import app.cash.turbine.test
import app.verdant.android.data.model.CreateMaintenanceRuleRequest
import app.verdant.android.data.model.MaintenanceRuleResponse
import app.verdant.android.data.model.MaintenanceTarget
import app.verdant.android.data.model.UpdateMaintenanceRuleRequest
import app.verdant.android.data.repository.MaintenanceRuleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MaintenanceRulesControllerTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun rule(id: Long = 1, activityType: String = "WEED") = MaintenanceRuleResponse(
        id = id, bedId = null, bedName = null,
        gardenAreaId = 5, gardenAreaName = "Gången",
        activityType = activityType, intervalDays = 21, anchorDate = null,
        seasonStartMonth = null, seasonStartDay = null,
        seasonEndMonth = null, seasonEndDay = null,
        active = true, notes = null,
        lastDoneDate = null, nextDueDate = "2026-06-25",
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private inner class FakeRuleRepository(
        var rules: MutableList<MaintenanceRuleResponse> = mutableListOf(),
        var failWith: Exception? = null,
    ) : MaintenanceRuleRepository {
        var lastListBedId: Long? = null
        var lastListAreaId: Long? = null
        var lastCreate: CreateMaintenanceRuleRequest? = null
        var lastUpdate: Pair<Long, UpdateMaintenanceRuleRequest>? = null
        var deletedIds = mutableListOf<Long>()

        override suspend fun list(bedId: Long?, areaId: Long?): List<MaintenanceRuleResponse> {
            failWith?.let { throw it }
            lastListBedId = bedId
            lastListAreaId = areaId
            return rules.toList()
        }
        override suspend fun create(request: CreateMaintenanceRuleRequest): MaintenanceRuleResponse {
            failWith?.let { throw it }
            lastCreate = request
            val created = rule(id = 99, activityType = request.activityType)
            rules.add(created)
            return created
        }
        override suspend fun update(id: Long, request: UpdateMaintenanceRuleRequest): MaintenanceRuleResponse {
            failWith?.let { throw it }
            lastUpdate = id to request
            return rules.first { it.id == id }
        }
        override suspend fun delete(id: Long) {
            failWith?.let { throw it }
            deletedIds.add(id)
            rules.removeAll { it.id == id }
        }
    }

    @Test
    fun `refresh loads rules scoped to an area, never with both filters`() = runTest {
        val repo = FakeRuleRepository(mutableListOf(rule()))
        val controller = MaintenanceRulesController(repo, MaintenanceTarget.AREA, 5, this)

        controller.refresh()
        advanceUntilIdle()

        controller.state.test {
            val s = awaitItem()
            assertEquals(1, s.rules.size)
            assertNull(s.error)
        }
        assertEquals(5L, repo.lastListAreaId)
        assertNull(repo.lastListBedId)
    }

    @Test
    fun `refresh on a bed target passes bedId only`() = runTest {
        val repo = FakeRuleRepository()
        val controller = MaintenanceRulesController(repo, MaintenanceTarget.BED, 3, this)

        controller.refresh()
        advanceUntilIdle()

        assertEquals(3L, repo.lastListBedId)
        assertNull(repo.lastListAreaId)
    }

    @Test
    fun `create sends exactly one target id and reloads`() = runTest {
        val repo = FakeRuleRepository()
        val controller = MaintenanceRulesController(repo, MaintenanceTarget.AREA, 5, this)

        controller.create(activityType = "MOW", intervalDays = 14)
        advanceUntilIdle()

        val sent = repo.lastCreate!!
        assertEquals(5L, sent.gardenAreaId)
        assertNull(sent.bedId)
        assertEquals("MOW", sent.activityType)
        assertEquals(14, sent.intervalDays)

        controller.state.test { assertEquals(1, awaitItem().rules.size) }
    }

    @Test
    fun `clearError dismisses a surfaced mutation failure`() = runTest {
        val repo = FakeRuleRepository(mutableListOf(rule()))
        val controller = MaintenanceRulesController(repo, MaintenanceTarget.AREA, 5, this)
        repo.failWith = RuntimeException("boom")

        controller.delete(1)
        advanceUntilIdle()
        assertNotNull(controller.state.value.error)

        controller.clearError()
        assertNull(controller.state.value.error)
    }

    @Test
    fun `delete removes the rule and reloads`() = runTest {
        val repo = FakeRuleRepository(mutableListOf(rule()))
        val controller = MaintenanceRulesController(repo, MaintenanceTarget.AREA, 5, this)

        controller.delete(1)
        advanceUntilIdle()

        assertEquals(listOf(1L), repo.deletedIds)
        controller.state.test { assertTrue(awaitItem().rules.isEmpty()) }
    }

    @Test
    fun `a load failure surfaces an error without clearing rules already shown`() = runTest {
        val repo = FakeRuleRepository(mutableListOf(rule()))
        val controller = MaintenanceRulesController(repo, MaintenanceTarget.AREA, 5, this)
        controller.refresh()
        advanceUntilIdle()

        repo.failWith = RuntimeException("nätverksfel")
        controller.refresh()
        advanceUntilIdle()

        controller.state.test {
            val s = awaitItem()
            assertNotNull(s.error)
            // Keeping the last good list matches BedDetailViewModel's
            // "once loaded, stay loaded" behaviour.
            assertEquals(1, s.rules.size)
        }
    }

    @Test
    fun `a create failure surfaces an error and adds no rule`() = runTest {
        val repo = FakeRuleRepository(failWith = RuntimeException("nätverksfel"))
        val controller = MaintenanceRulesController(repo, MaintenanceTarget.AREA, 5, this)

        controller.create(activityType = "MOW", intervalDays = 14)
        advanceUntilIdle()

        controller.state.test {
            val s = awaitItem()
            assertNotNull(s.error)
            assertTrue(s.rules.isEmpty())
        }
    }

    @Test
    fun `an update failure surfaces an error`() = runTest {
        val repo = FakeRuleRepository(mutableListOf(rule()), failWith = RuntimeException("nätverksfel"))
        val controller = MaintenanceRulesController(repo, MaintenanceTarget.AREA, 5, this)

        controller.update(1, UpdateMaintenanceRuleRequest(intervalDays = 30))
        advanceUntilIdle()

        controller.state.test { assertNotNull(awaitItem().error) }
    }

    @Test
    fun `a delete failure surfaces an error without removing the rule`() = runTest {
        val repo = FakeRuleRepository(mutableListOf(rule()))
        val controller = MaintenanceRulesController(repo, MaintenanceTarget.AREA, 5, this)
        controller.refresh()
        advanceUntilIdle()

        repo.failWith = RuntimeException("nätverksfel")
        controller.delete(1)
        advanceUntilIdle()

        controller.state.test {
            val s = awaitItem()
            assertNotNull(s.error)
            assertEquals(1, s.rules.size)
        }
    }

    @Test
    fun `a subsequent successful mutation clears a previous error`() = runTest {
        val repo = FakeRuleRepository(mutableListOf(rule()), failWith = RuntimeException("nätverksfel"))
        val controller = MaintenanceRulesController(repo, MaintenanceTarget.AREA, 5, this)
        controller.delete(1)
        advanceUntilIdle()
        controller.state.test { assertNotNull(awaitItem().error) }

        repo.failWith = null
        controller.delete(1)
        advanceUntilIdle()

        controller.state.test { assertNull(awaitItem().error) }
    }

    @Test
    fun `clearError resets the error without touching rules`() = runTest {
        val repo = FakeRuleRepository(mutableListOf(rule()))
        val controller = MaintenanceRulesController(repo, MaintenanceTarget.AREA, 5, this)
        controller.refresh()
        advanceUntilIdle()

        repo.failWith = RuntimeException("nätverksfel")
        controller.refresh()
        advanceUntilIdle()
        controller.state.test { assertNotNull(awaitItem().error) }

        controller.clearError()

        controller.state.test {
            val s = awaitItem()
            assertNull(s.error)
            assertEquals(1, s.rules.size)
        }
    }
}

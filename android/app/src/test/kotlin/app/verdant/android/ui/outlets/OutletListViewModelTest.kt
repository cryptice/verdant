package app.verdant.android.ui.outlets

import app.verdant.android.data.model.CreateOutletRequest
import app.verdant.android.data.model.OutletResponse
import app.verdant.android.data.model.UpdateOutletRequest
import app.verdant.android.data.repository.OutletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class OutletListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `load populates items`() = runTest {
        val outlets = listOf(outlet(1, "Saluhallen"), outlet(2, "Florist"))
        val vm = OutletListViewModel(FakeOutletRepo(outlets))
        advanceUntilIdle()
        assertEquals(outlets, vm.uiState.value.items)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `load surfaces error on failure`() = runTest {
        val vm = OutletListViewModel(FakeOutletRepo(throwOnList = RuntimeException("boom")))
        advanceUntilIdle()
        assertEquals("boom", vm.uiState.value.error)
        assertTrue(vm.uiState.value.items.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `create appends to state via reload`() = runTest {
        val repo = FakeOutletRepo(emptyList())
        repo.afterCreate = listOf(outlet(99, "Ny"))
        val vm = OutletListViewModel(repo)
        advanceUntilIdle()
        vm.create("Ny", "FLORIST")
        advanceUntilIdle()
        assertEquals(listOf(99L), vm.uiState.value.items.map { it.id })
        assertEquals(listOf("Ny" to "FLORIST"), repo.created)
    }

    @Test
    fun `delete removes from state via reload`() = runTest {
        val repo = FakeOutletRepo(listOf(outlet(1, "A"), outlet(2, "B")))
        repo.afterDelete = listOf(outlet(2, "B"))
        val vm = OutletListViewModel(repo)
        advanceUntilIdle()
        vm.delete(1L)
        advanceUntilIdle()
        assertEquals(listOf(2L), vm.uiState.value.items.map { it.id })
        assertEquals(listOf(1L), repo.deleted)
    }

    private fun outlet(id: Long, name: String) = OutletResponse(
        id = id, name = name, channel = "FLORIST",
        contactInfo = null, notes = null,
        createdAt = "2026-05-18T00:00:00Z", updatedAt = "2026-05-18T00:00:00Z",
    )
}

private class FakeOutletRepo(
    private val initial: List<OutletResponse> = emptyList(),
    private val throwOnList: Throwable? = null,
) : OutletRepository {
    val created = mutableListOf<Pair<String, String>>()
    val deleted = mutableListOf<Long>()
    var afterCreate: List<OutletResponse>? = null
    var afterDelete: List<OutletResponse>? = null

    override suspend fun list(): List<OutletResponse> {
        throwOnList?.let { throw it }
        return when {
            created.isNotEmpty() && afterCreate != null -> afterCreate!!
            deleted.isNotEmpty() && afterDelete != null -> afterDelete!!
            else -> initial
        }
    }

    override suspend fun create(request: CreateOutletRequest): OutletResponse {
        created += request.name to request.channel
        return OutletResponse(
            id = 99L, name = request.name, channel = request.channel,
            contactInfo = null, notes = null,
            createdAt = "2026-05-18T00:00:00Z", updatedAt = "2026-05-18T00:00:00Z",
        )
    }

    override suspend fun update(id: Long, request: UpdateOutletRequest): OutletResponse =
        error("not used")

    override suspend fun delete(id: Long): Response<Unit> {
        deleted += id
        return Response.success(Unit)
    }
}

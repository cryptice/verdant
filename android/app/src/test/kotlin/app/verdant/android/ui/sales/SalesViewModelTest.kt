package app.verdant.android.ui.sales

import app.verdant.android.data.model.CreateSaleLotForHarvestRequest
import app.verdant.android.data.model.CreateSaleLotForPlantRequest
import app.verdant.android.data.model.CreateSeasonRequest
import app.verdant.android.data.model.EditSaleRequest
import app.verdant.android.data.model.RecordSaleRequest
import app.verdant.android.data.model.SaleLedgerEntry
import app.verdant.android.data.model.SaleLotDetailResponse
import app.verdant.android.data.model.SaleLotResponse
import app.verdant.android.data.model.SaleResponse
import app.verdant.android.data.model.SeasonResponse
import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.repository.CustomerRepository
import app.verdant.android.data.repository.SaleLotRepository
import app.verdant.android.data.repository.SaleRepository
import app.verdant.android.data.repository.SeasonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class SalesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loadLedger populates ledger`() = runTest {
        val entries = listOf(
            ledgerEntry(id = 1, total = 5000),
            ledgerEntry(id = 2, total = 8000),
        )
        val vm = SalesViewModel(
            FakeSaleLotRepo(),
            FakeSaleRepo(ledger = entries),
            FakeSeasonRepo(),
            stubCustomerRepo(),
        )
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals(2, s.ledger.size)
    }

    @Test
    fun `default falls back to most recent season when none matches today`() = runTest {
        val seasons = listOf(
            season(id = 1, startDate = "2024-03-01", endDate = "2024-11-30"),
            season(id = 2, startDate = "2023-03-01", endDate = "2023-11-30"),
        )
        val vm = SalesViewModel(FakeSaleLotRepo(), FakeSaleRepo(), FakeSeasonRepo(seasons), stubCustomerRepo())
        advanceUntilIdle()
        assertEquals(1L, vm.uiState.value.selectedSeasonId)
    }

    @Test
    fun `default is null when no seasons exist`() = runTest {
        val vm = SalesViewModel(FakeSaleLotRepo(), FakeSaleRepo(), FakeSeasonRepo(emptyList()), stubCustomerRepo())
        advanceUntilIdle()
        assertNull(vm.uiState.value.selectedSeasonId)
    }

    @Test
    fun `selectSeason updates state and re-fetches`() = runTest {
        val repo = FakeSaleRepo(ledger = listOf(ledgerEntry(id = 1, total = 1000)))
        val vm = SalesViewModel(FakeSaleLotRepo(), repo, FakeSeasonRepo(), stubCustomerRepo())
        advanceUntilIdle()
        vm.selectSeason(42L)
        advanceUntilIdle()
        assertEquals(42L, vm.uiState.value.selectedSeasonId)
        assertEquals(listOf(null, 42L), repo.requestedSeasonIds)
    }

    /** Builds a CustomerRepository over a JDK proxy VerdantApi that throws on any call.
     *  loadCustomers() swallows the exception, so customers stay empty in tests. */
    private fun stubCustomerRepo(): CustomerRepository {
        val api = java.lang.reflect.Proxy.newProxyInstance(
            VerdantApi::class.java.classLoader,
            arrayOf(VerdantApi::class.java),
        ) { _, _, _ -> throw UnsupportedOperationException("not used") } as VerdantApi
        return CustomerRepository(api)
    }

    private fun ledgerEntry(id: Long, total: Int) = SaleLedgerEntry(
        id = id, saleLotId = id, sourceKind = "PLANT", sourceSummary = "Zinnia",
        unitKind = "STEM", quantity = 1, pricePerUnitCents = total, totalCents = total,
        outletName = "Saluhallen", customerName = null, soldAt = "2026-05-12",
    )

    private fun season(id: Long, startDate: String, endDate: String) = SeasonResponse(
        id = id, name = "Season $id", year = 2024 + id.toInt(),
        startDate = startDate, endDate = endDate,
        lastFrostDate = null, firstFrostDate = null,
        growingDegreeBaseC = 10.0, notes = null, isActive = true,
        createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z",
    )
}

private class FakeSaleLotRepo : SaleLotRepository {
    override suspend fun list(status: String?, sourceKind: String?): List<SaleLotResponse> = emptyList()
    override suspend fun detail(id: Long): SaleLotDetailResponse = error("not used")
    override suspend fun createForPlant(request: CreateSaleLotForPlantRequest): SaleLotResponse = error("not used")
    override suspend fun createForHarvest(request: CreateSaleLotForHarvestRequest): SaleLotResponse = error("not used")
    override suspend fun changePrice(id: Long, newPriceCents: Int): SaleLotResponse = error("not used")
    override suspend fun changeOutlet(id: Long, newOutletId: Long): SaleLotResponse = error("not used")
    override suspend fun markReturnedFromOutlet(id: Long, fromOutletId: Long): Response<Unit> = error("not used")
    override suspend fun markNotSold(id: Long): SaleLotResponse = error("not used")
    override suspend fun delete(id: Long): Response<Unit> = error("not used")
    override suspend fun availableForPlant(plantId: Long): Int = 0
    override suspend fun availableForHarvestEvent(eventId: Long): Int = 0
    override suspend fun availableForBouquet(bouquetId: Long): Int = 0
}

private class FakeSaleRepo(
    private val ledger: List<SaleLedgerEntry> = emptyList(),
) : SaleRepository {
    val requestedSeasonIds = mutableListOf<Long?>()
    override suspend fun listLedger(seasonId: Long?, limit: Int, offset: Int): List<SaleLedgerEntry> {
        requestedSeasonIds += seasonId
        return ledger
    }
    override suspend fun record(lotId: Long, request: RecordSaleRequest): SaleResponse = error("not used")
    override suspend fun edit(saleId: Long, request: EditSaleRequest): SaleResponse = error("not used")
}

private class FakeSeasonRepo(
    private val seasons: List<SeasonResponse> = emptyList(),
) : SeasonRepository {
    override suspend fun list(): List<SeasonResponse> = seasons
    override suspend fun create(request: CreateSeasonRequest): SeasonResponse = error("not used")
    override suspend fun update(id: Long, request: Map<String, Any?>): SeasonResponse = error("not used")
    override suspend fun delete(id: Long) { error("not used") }
}

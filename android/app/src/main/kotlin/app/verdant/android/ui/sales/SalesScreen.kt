package app.verdant.android.ui.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CustomerResponse
import app.verdant.android.data.model.EditSaleRequest
import app.verdant.android.data.model.SaleLedgerEntry
import app.verdant.android.data.model.SaleLotResponse
import app.verdant.android.data.model.SaleLotStatus
import app.verdant.android.data.model.SeasonResponse
import app.verdant.android.data.repository.CustomerRepository
import app.verdant.android.data.repository.OutletRepository
import app.verdant.android.data.repository.SaleLotRepository
import app.verdant.android.data.repository.SaleRepository
import app.verdant.android.data.repository.SeasonRepository
import app.verdant.android.data.repository.SpeciesRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.plant.unitLabelSv
import app.verdant.android.ui.faltet.BotanicalPlate
import app.verdant.android.ui.faltet.FaltetDropdown
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SalesState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val lots: List<SaleLotResponse> = emptyList(),
    val seasons: List<SeasonResponse> = emptyList(),
    val selectedSeasonId: Long? = null,
    val ledger: List<SaleLedgerEntry> = emptyList(),
    val ledgerLoading: Boolean = false,
    val ledgerError: String? = null,
    val customers: List<CustomerResponse> = emptyList(),
    val species: List<app.verdant.android.data.model.SpeciesResponse> = emptyList(),
    val outlets: List<app.verdant.android.data.model.OutletResponse> = emptyList(),
)

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val saleLotRepository: SaleLotRepository,
    private val saleRepository: SaleRepository,
    private val seasonRepository: SeasonRepository,
    private val customerRepository: CustomerRepository,
    private val speciesRepository: SpeciesRepository,
    private val outletRepository: OutletRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SalesState())
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
        loadSeasons()
        loadCustomers()
        loadSpecies()
        loadOutlets()
    }

    fun loadSpecies() {
        viewModelScope.launch {
            try {
                val list = speciesRepository.list()
                _uiState.value = _uiState.value.copy(species = list)
            } catch (e: Exception) {
                // Silent — quick-sale dialog shows an empty species list.
            }
        }
    }

    fun loadOutlets() {
        viewModelScope.launch {
            try {
                val list = outletRepository.list()
                _uiState.value = _uiState.value.copy(outlets = list)
            } catch (e: Exception) {
                // Silent — dialog shows empty outlet list.
            }
        }
    }

    fun recordQuickSale(request: app.verdant.android.data.model.QuickSaleRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                saleRepository.recordQuick(request)
                refresh()
                loadLedger()
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(ledgerError = e.message)
            }
        }
    }

    fun createOutlet(name: String, channel: String) {
        viewModelScope.launch {
            try {
                val created = outletRepository.create(
                    app.verdant.android.data.model.CreateOutletRequest(name = name, channel = channel)
                )
                _uiState.value = _uiState.value.copy(outlets = _uiState.value.outlets + created)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(ledgerError = e.message)
            }
        }
    }

    fun loadCustomers() {
        viewModelScope.launch {
            try {
                val list = customerRepository.list()
                _uiState.value = _uiState.value.copy(customers = list)
            } catch (e: Exception) {
                // Silent — customer picker just shows nothing.
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val lots = saleLotRepository.list()
                _uiState.value = _uiState.value.copy(isLoading = false, lots = lots)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadSeasons() {
        viewModelScope.launch {
            try {
                val seasons = seasonRepository.list()
                val today = LocalDate.now()
                val defaultSeason = seasons.firstOrNull { s ->
                    val start = s.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    val end = s.endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    (start == null || !today.isBefore(start)) && (end == null || !today.isAfter(end))
                } ?: seasons.maxByOrNull { it.startDate ?: "" }
                _uiState.value = _uiState.value.copy(
                    seasons = seasons,
                    selectedSeasonId = defaultSeason?.id,
                )
                loadLedger()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(ledgerError = e.message)
            }
        }
    }

    fun selectSeason(seasonId: Long?) {
        _uiState.value = _uiState.value.copy(selectedSeasonId = seasonId)
        loadLedger()
    }

    fun loadLedger() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ledgerLoading = true, ledgerError = null)
            try {
                val entries = saleRepository.listLedger(_uiState.value.selectedSeasonId)
                _uiState.value = _uiState.value.copy(ledger = entries, ledgerLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(ledgerLoading = false, ledgerError = e.message)
            }
        }
    }

    fun editSale(saleId: Long, request: EditSaleRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                saleRepository.edit(saleId, request)
                loadLedger()
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(ledgerError = e.message)
            }
        }
    }
}

@Composable
fun SalesScreen(
    onLotClick: (Long) -> Unit,
    viewModel: SalesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var tabIndex by remember { mutableIntStateOf(0) }
    var editingEntry by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<SaleLedgerEntry?>(null) }
    var showQuickSaleDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val tabs: List<Pair<String?, String>> = listOf(
        SaleLotStatus.OFFERED to "Aktiva",
        SaleLotStatus.SOLD_OUT to "Sålda",
        SaleLotStatus.NOT_SOLD to "Ej sålda",
        null to "Försäljningar",
    )

    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(tabIndex) {
        if (tabs[tabIndex].first == null && uiState.ledger.isEmpty() && !uiState.ledgerLoading) {
            viewModel.loadLedger()
        }
    }

    val visibleLots = remember(uiState.lots, tabIndex) {
        val status = tabs[tabIndex].first ?: return@remember emptyList()
        uiState.lots.filter { it.status == status }
    }
    val isLedgerTab = tabs[tabIndex].first == null

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Försäljning",
        watermark = BotanicalPlate.Harvest,
        fab = {
            FaltetFab(
                onClick = { showQuickSaleDialog = true },
                contentDescription = "Lägg till försäljning",
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null && uiState.lots.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { ConnectionErrorState(onRetry = { viewModel.refresh() }) }
            else -> {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    TabRow(selectedTabIndex = tabIndex, containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEachIndexed { i, (_, label) ->
                            Tab(selected = tabIndex == i, onClick = { tabIndex = i }, text = { Text(label) })
                        }
                    }
                    if (isLedgerTab) {
                        LedgerTabContent(
                            seasons = uiState.seasons,
                            selectedSeasonId = uiState.selectedSeasonId,
                            ledger = uiState.ledger,
                            loading = uiState.ledgerLoading,
                            error = uiState.ledgerError,
                            onSeasonSelected = { viewModel.selectSeason(it) },
                            onEntryClick = { entry -> editingEntry = entry },
                            onRetry = { viewModel.loadLedger() },
                        )
                    } else if (visibleLots.isEmpty()) {
                        FaltetEmptyState(
                            headline = when (tabs[tabIndex].first) {
                                SaleLotStatus.OFFERED -> "Inga aktiva försäljningar"
                                SaleLotStatus.SOLD_OUT -> "Inget sålt ännu"
                                else -> "Inga oförsålda"
                            },
                            subtitle = "Lägg ut en planta, skörd eller bukett till försäljning för att börja.",
                            modifier = Modifier.padding(top = 32.dp),
                        )
                    } else {
                        LazyColumn {
                            items(visibleLots, key = { it.id }) { lot ->
                                SaleLotRow(lot = lot, onClick = { onLotClick(lot.id) })
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }

    editingEntry?.let { entry ->
        EditSaleDialog(
            entry = entry,
            customers = uiState.customers,
            initialCustomerId = entry.customerId,
            onDismiss = { editingEntry = null },
            onConfirm = { request ->
                viewModel.editSale(entry.id, request) { editingEntry = null }
            },
        )
    }

    if (showQuickSaleDialog) {
        QuickSaleDialog(
            species = uiState.species,
            outlets = uiState.outlets,
            customers = uiState.customers,
            onDismiss = { showQuickSaleDialog = false },
            onConfirm = { request ->
                viewModel.recordQuickSale(request) { showQuickSaleDialog = false }
            },
            onCreateOutlet = { name, channel -> viewModel.createOutlet(name, channel) },
        )
    }
}

@Composable
private fun SaleLotRow(lot: SaleLotResponse, onClick: () -> Unit) {
    FaltetListRow(
        title = lot.sourceSummary ?: sourceKindLabelSv(lot.sourceKind),
        meta = buildString {
            append("${lot.quantityRemaining}/${lot.quantityTotal} ${unitLabelSv(lot.unitKind).lowercase()}")
            append(" · ${lot.currentOutletName}")
            if (lot.currentRequestedPriceCents != lot.initialRequestedPriceCents) {
                append(" · sänkt")
            }
        },
        stat = {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "%.2f".format(lot.currentRequestedPriceCents / 100.0),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    color = FaltetInk,
                )
                Text(" KR", fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 1.2.sp, color = FaltetForest)
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun LedgerTabContent(
    seasons: List<SeasonResponse>,
    selectedSeasonId: Long?,
    ledger: List<SaleLedgerEntry>,
    loading: Boolean,
    error: String?,
    onSeasonSelected: (Long?) -> Unit,
    onEntryClick: (SaleLedgerEntry) -> Unit,
    onRetry: () -> Unit,
) {
    androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
        // Season chip
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Säsong:", fontSize = 12.sp, color = FaltetForest)
            Spacer(Modifier.width(8.dp))
            val seasonOptions: List<SeasonChoice> =
                listOf(SeasonChoice.All) + seasons.map { SeasonChoice.Season(it) }
            val selectedChoice: SeasonChoice = seasonOptions.firstOrNull {
                it is SeasonChoice.Season && it.value.id == selectedSeasonId
            } ?: SeasonChoice.All
            Box(Modifier.weight(1f)) {
                FaltetDropdown(
                    label = "",
                    options = seasonOptions,
                    selected = selectedChoice,
                    onSelectedChange = { onSeasonSelected((it as? SeasonChoice.Season)?.value?.id) },
                    labelFor = { it.label },
                    searchable = false,
                )
            }
        }

        // Summary
        val totalKronor = ledger.sumOf { it.totalCents } / 100
        Text(
            text = "$totalKronor KR · ${ledger.size} försäljningar",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            letterSpacing = 1.2.sp,
            color = FaltetForest,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
        )

        when {
            loading -> FaltetLoadingState(Modifier.weight(1f))
            error != null && ledger.isEmpty() -> Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) { ConnectionErrorState(onRetry = onRetry) }
            ledger.isEmpty() -> FaltetEmptyState(
                headline = "Inga försäljningar",
                subtitle = "Registrera en försäljning på en bukett, planta eller skörd för att se den här.",
                modifier = Modifier.padding(top = 32.dp),
            )
            else -> LazyColumn(Modifier.weight(1f)) {
                items(ledger, key = { it.id }) { entry ->
                    SaleLedgerRow(entry = entry, onClick = { onEntryClick(entry) })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

private sealed class SeasonChoice {
    abstract val label: String

    object All : SeasonChoice() {
        override val label: String = "Allt"
    }

    data class Season(val value: SeasonResponse) : SeasonChoice() {
        override val label: String get() = value.name
    }
}

@Composable
private fun SaleLedgerRow(entry: SaleLedgerEntry, onClick: () -> Unit) {
    FaltetListRow(
        title = entry.sourceSummary ?: sourceKindLabelSv(entry.sourceKind),
        meta = buildString {
            append("${entry.quantity} ${unitLabelSv(entry.unitKind).lowercase()}")
            append(" · ${formatSoldAt(entry.soldAt)}")
            entry.customerName?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
            append(" · ${entry.outletName}")
        },
        stat = {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = (entry.totalCents / 100).toString(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    color = FaltetInk,
                )
                Text(" KR", fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 1.2.sp, color = FaltetForest)
            }
        },
        onClick = onClick,
    )
}

private val ledgerSvDateFormatter =
    java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale("sv"))

private fun formatSoldAt(iso: String): String = runCatching {
    java.time.LocalDate.parse(iso.take(10)).format(ledgerSvDateFormatter)
}.getOrElse { iso }

internal fun sourceKindLabelSv(kind: String): String = when (kind) {
    app.verdant.android.data.model.SourceKind.PLANT -> "Plantor"
    app.verdant.android.data.model.SourceKind.HARVEST_EVENT -> "Skörd"
    app.verdant.android.data.model.SourceKind.BOUQUET -> "Bukett"
    else -> kind
}

internal fun statusLabelSv(status: String): String = when (status) {
    SaleLotStatus.OFFERED -> "Aktiv"
    SaleLotStatus.SOLD_OUT -> "Slutsåld"
    SaleLotStatus.NOT_SOLD -> "Ej såld"
    else -> status
}

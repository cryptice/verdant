package app.verdant.android.ui.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import app.verdant.android.data.model.SaleLotResponse
import app.verdant.android.data.model.SaleLotStatus
import app.verdant.android.data.repository.SaleLotRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.plant.unitLabelSv
import app.verdant.android.ui.faltet.BotanicalPlate
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SalesState(
    val isLoading: Boolean = true,
    val lots: List<SaleLotResponse> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val saleLotRepository: SaleLotRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SalesState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val lots = saleLotRepository.list()
                _uiState.value = SalesState(isLoading = false, lots = lots)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
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
    val tabs = listOf(
        SaleLotStatus.OFFERED to "Aktiva",
        SaleLotStatus.SOLD_OUT to "Sålda",
        SaleLotStatus.NOT_SOLD to "Ej sålda",
    )

    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val visibleLots = remember(uiState.lots, tabIndex) {
        uiState.lots.filter { it.status == tabs[tabIndex].first }
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Försäljning",
        watermark = BotanicalPlate.Harvest,
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
                    if (visibleLots.isEmpty()) {
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

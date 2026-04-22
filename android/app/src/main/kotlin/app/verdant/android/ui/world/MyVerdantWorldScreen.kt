package app.verdant.android.ui.world

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.DashboardResponse
import app.verdant.android.data.model.HarvestStatRow
import app.verdant.android.data.model.TraySummaryEntry
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
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
import javax.inject.Inject

private const val TAG = "MyVerdantWorldScreen"

data class MyWorldState(
    val isLoading: Boolean = true,
    val dashboard: DashboardResponse? = null,
    val harvestStats: List<HarvestStatRow> = emptyList(),
    val trayPlants: List<TraySummaryEntry> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class MyWorldViewModel @Inject constructor(
    private val repo: GardenRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyWorldState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val showLoading = _uiState.value.dashboard == null
            if (showLoading) _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val dashboard = repo.getDashboard()
                val stats = repo.getHarvestStats()
                val tray = try { repo.getTraySummary() } catch (e: Exception) { Log.e(TAG, "Failed to load tray summary", e); emptyList() }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    dashboard = dashboard,
                    harvestStats = stats,
                    trayPlants = tray,
                )
            } catch (e: Exception) {
                if (showLoading) _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyVerdantWorldScreen(
    onGardenClick: (Long) -> Unit,
    onCreateGarden: () -> Unit,
    viewModel: MyWorldViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    FaltetScreenScaffold(
        mastheadLeft = "§ Min värld",
        mastheadCenter = "Trädgårdar",
        fab = { FaltetFab(onClick = onCreateGarden, contentDescription = "Skapa trädgård") },
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null -> ConnectionErrorState(onRetry = { viewModel.refresh() })
            uiState.dashboard!!.gardens.isEmpty() -> FaltetEmptyState(
                headline = "Inga trädgårdar",
                subtitle = "Skapa din första trädgård.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(uiState.dashboard!!.gardens, key = { it.id }) { garden ->
                    FaltetListRow(
                        title = garden.name,
                        leading = null,
                        meta = null,
                        stat = {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = garden.bedCount.toString(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp,
                                    color = FaltetInk,
                                )
                                Text(
                                    text = " BÄDDAR",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.2.sp,
                                    color = FaltetForest,
                                )
                            }
                        },
                        actions = null,
                        onClick = { onGardenClick(garden.id) },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun MyVerdantWorldScreenPreview() {
    FaltetEmptyState(
        headline = "Inga trädgårdar",
        subtitle = "Skapa din första trädgård.",
    )
}

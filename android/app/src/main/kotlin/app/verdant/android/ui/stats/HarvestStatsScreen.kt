package app.verdant.android.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.HarvestStatRow
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.theme.verdantTopAppBarColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HarvestStatsState(
    val isLoading: Boolean = true,
    val stats: List<HarvestStatRow> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HarvestStatsViewModel @Inject constructor(
    private val gardenRepository: GardenRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HarvestStatsState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = HarvestStatsState(isLoading = true)
            try {
                val stats = gardenRepository.getHarvestStats()
                _uiState.value = HarvestStatsState(isLoading = false, stats = stats)
            } catch (e: Exception) {
                _uiState.value = HarvestStatsState(isLoading = false, error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HarvestStatsScreen(
    viewModel: HarvestStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.harvest_stats)) }, colors = verdantTopAppBarColors())
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.stats.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Agriculture, null, Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.no_harvests_yet), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(stringResource(R.string.harvest_events_hint),
                        fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(uiState.stats) { stat ->
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(stat.species, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatItem("Weight", formatWeight(stat.totalWeightGrams))
                                StatItem("Quantity", stat.totalQuantity.toString())
                                StatItem("Harvests", stat.harvestCount.toString())
                            }
                        }
                    }
                }
            }
        }
        if (uiState.error != null && !uiState.isLoading && uiState.stats.isEmpty()) {
            app.verdant.android.ui.common.ConnectionErrorState(
                onRetry = { viewModel.refresh() },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

private fun formatWeight(grams: Double): String {
    return if (grams >= 1000) {
        "${"%.1f".format(grams / 1000)}kg"
    } else {
        "${"%.0f".format(grams)}g"
    }
}

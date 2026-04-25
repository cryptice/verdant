package app.verdant.android.ui.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.verdant.android.R
import app.verdant.android.ui.theme.verdantTopAppBarColors
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.PlantResponse
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlantPickerState(
    val isLoading: Boolean = true,
    val plants: List<PlantResponse> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class PlantPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: GardenRepository,
) : ViewModel() {
    private val statuses: String? = savedStateHandle.get<String>("statuses")
    private val speciesId: Long? = savedStateHandle.get<Long>("speciesId")?.takeIf { it > 0 }
    private val _uiState = MutableStateFlow(PlantPickerState())
    val uiState = _uiState.asStateFlow()

    init { loadPlants() }

    private fun loadPlants() {
        viewModelScope.launch {
            _uiState.value = PlantPickerState(isLoading = true)
            try {
                val allPlants = repo.getAllPlants()
                var filtered = if (statuses != null) {
                    val allowed = statuses.split(",").toSet()
                    allPlants.filter { it.status in allowed }
                } else allPlants
                // Filter by species when launched from a scheduled task
                if (speciesId != null) {
                    filtered = filtered.filter { it.speciesId == speciesId }
                }
                _uiState.value = PlantPickerState(isLoading = false, plants = filtered)
            } catch (e: Exception) {
                _uiState.value = PlantPickerState(isLoading = false, error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantPickerScreen(
    onBack: () -> Unit,
    onPlantSelected: (Long) -> Unit,
    viewModel: PlantPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredPlants = run {
        val tokens = searchQuery.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) uiState.plants
        else uiState.plants.filter { plant ->
            val haystack = listOfNotNull(plant.name, plant.speciesName).joinToString(" ").lowercase()
            tokens.all { haystack.contains(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.select_plant)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = verdantTopAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_plants)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.error != null -> {
                    app.verdant.android.ui.common.ConnectionErrorState(
                        onRetry = onBack
                    )
                }
                filteredPlants.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_plants_found), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredPlants) { plant ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onPlantSelected(plant.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(plant.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    plant.speciesName?.let {
                                        Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Text(
                                        plant.status.replace("_", " "),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

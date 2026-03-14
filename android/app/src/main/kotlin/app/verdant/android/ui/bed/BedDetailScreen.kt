package app.verdant.android.ui.bed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.BedResponse
import app.verdant.android.data.model.PlantResponse
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BedDetailState(
    val isLoading: Boolean = true,
    val bed: BedResponse? = null,
    val plants: List<PlantResponse> = emptyList(),
    val error: String? = null,
    val deleted: Boolean = false
)

@HiltViewModel
class BedDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gardenRepository: GardenRepository
) : ViewModel() {
    private val bedId: Long = savedStateHandle.get<Long>("bedId")!!
    private val _uiState = MutableStateFlow(BedDetailState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = BedDetailState(isLoading = true)
            try {
                val bed = gardenRepository.getBed(bedId)
                val plants = gardenRepository.getPlants(bedId)
                _uiState.value = BedDetailState(isLoading = false, bed = bed, plants = plants)
            } catch (e: Exception) {
                _uiState.value = BedDetailState(isLoading = false, error = e.message)
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                gardenRepository.deleteBed(bedId)
                _uiState.value = _uiState.value.copy(deleted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BedDetailScreen(
    onBack: () -> Unit,
    onPlantClick: (Long) -> Unit,
    onCreatePlant: (Long) -> Unit,
    viewModel: BedDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onBack()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_bed)) },
            text = { Text(stringResource(R.string.delete_bed_confirm)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.delete() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.bed?.name ?: stringResource(R.string.bed)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            uiState.bed?.let { bed ->
                FloatingActionButton(
                    onClick = { onCreatePlant(bed.id) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.add_plant))
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> {
                app.verdant.android.ui.common.ConnectionErrorState(
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.bed?.let { bed ->
                        item {
                            Spacer(Modifier.height(4.dp))
                            bed.description?.let {
                                Text(it, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                                Spacer(Modifier.height(16.dp))
                            }
                            Text(stringResource(R.string.plants), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }

                    if (uiState.plants.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.no_plants_yet),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }

                    items(uiState.plants) { plant ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onPlantClick(plant.id) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(plant.name, fontWeight = FontWeight.SemiBold)
                                    plant.speciesName?.let {
                                        Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                                AssistChip(
                                    onClick = { },
                                    label = { Text(plant.status, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

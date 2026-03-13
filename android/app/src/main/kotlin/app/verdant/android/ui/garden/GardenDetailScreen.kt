package app.verdant.android.ui.garden

import android.util.Log
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.BedResponse
import app.verdant.android.data.model.GardenResponse
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "GardenDetail"

data class GardenDetailState(
    val isLoading: Boolean = true,
    val garden: GardenResponse? = null,
    val beds: List<BedResponse> = emptyList(),
    val error: String? = null,
    val deleted: Boolean = false
)

@HiltViewModel
class GardenDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gardenRepository: GardenRepository
) : ViewModel() {
    private val gardenId: Long = savedStateHandle.get<Long>("gardenId")!!
    private val _uiState = MutableStateFlow(GardenDetailState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = GardenDetailState(isLoading = true)
            // Retry up to 3 times with delay — garden may not be visible immediately after creation
            var lastError: Exception? = null
            for (attempt in 1..3) {
                try {
                    Log.d(TAG, "Loading garden $gardenId (attempt $attempt)")
                    val garden = gardenRepository.getGarden(gardenId)
                    Log.d(TAG, "Garden loaded: ${garden.name}")
                    val beds = gardenRepository.getBeds(gardenId)
                    Log.d(TAG, "Beds loaded: ${beds.size}")
                    _uiState.value = GardenDetailState(isLoading = false, garden = garden, beds = beds)
                    return@launch
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load garden (attempt $attempt): ${e.message}")
                    lastError = e
                    if (attempt < 3) kotlinx.coroutines.delay(500L * attempt)
                }
            }
            _uiState.value = GardenDetailState(isLoading = false, error = lastError?.message)
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                gardenRepository.deleteGarden(gardenId)
                _uiState.value = _uiState.value.copy(deleted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenDetailScreen(
    onBack: () -> Unit,
    onBedClick: (Long) -> Unit,
    onCreateBed: (Long) -> Unit,
    viewModel: GardenDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onBack()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Garden") },
            text = { Text("This will delete the garden and all its beds and plants. Are you sure?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.delete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.garden?.name ?: "Garden") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            uiState.garden?.let { garden ->
                FloatingActionButton(
                    onClick = { onCreateBed(garden.id) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "Add Bed")
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Failed to load garden", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(4.dp))
                    Text(uiState.error ?: "", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.refresh() }) { Text("Retry") }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.garden?.let { garden ->
                        item {
                            Spacer(Modifier.height(4.dp))
                            Text(garden.emoji ?: "\uD83C\uDF31", fontSize = 48.sp)
                            garden.description?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(it, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Beds", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }

                    if (uiState.beds.isEmpty()) {
                        item {
                            Text(
                                "No beds yet. Tap + to add one.",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }

                    items(uiState.beds) { bed ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onBedClick(bed.id) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(bed.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                bed.description?.let {
                                    Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

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
import app.verdant.android.data.model.UpdateGardenRequest
import app.verdant.android.ui.theme.verdantTopAppBarColors
import app.verdant.android.data.model.GardenResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.bed.bedDrainageLabel
import app.verdant.android.ui.bed.bedProtectionLabel
import app.verdant.android.ui.bed.bedSunExposureLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "GardenDetail"

@Composable
private fun BedConditionChips(bed: BedResponse) {
    val sunLabel = bed.sunExposure?.let { bedSunExposureLabel(it) }
    val drainageLabel = bed.drainage?.let { bedDrainageLabel(it) }
    val protectionLabel = bed.protection?.let { bedProtectionLabel(it) }

    if (sunLabel == null && drainageLabel == null && protectionLabel == null) return

    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        sunLabel?.let {
            AssistChip(
                onClick = {},
                label = { Text("\u2600\uFE0F $it", fontSize = 11.sp) }
            )
        }
        drainageLabel?.let {
            AssistChip(
                onClick = {},
                label = { Text("\uD83D\uDCA7 $it", fontSize = 11.sp) }
            )
        }
        protectionLabel?.let {
            AssistChip(
                onClick = {},
                label = { Text("\uD83C\uDFE0 $it", fontSize = 11.sp) }
            )
        }
    }
}

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
                    val beds = gardenRepository.getBeds(gardenId).sortedBy { it.name.lowercase() }
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

    fun update(name: String, description: String?, emoji: String?) {
        viewModelScope.launch {
            try {
                gardenRepository.updateGarden(gardenId, UpdateGardenRequest(name = name, description = description, emoji = emoji))
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
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
    var editing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editEmoji by remember { mutableStateOf("") }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onBack()
    }

    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(R.string.edit)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(R.string.garden_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text(stringResource(R.string.description_optional)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2
                    )
                    OutlinedTextField(
                        value = editEmoji,
                        onValueChange = { editEmoji = it },
                        label = { Text(stringResource(R.string.emoji)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.update(editName, editDescription.ifBlank { null }, editEmoji.ifBlank { null })
                        editing = false
                    },
                    enabled = editName.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_garden)) },
            text = { Text(stringResource(R.string.delete_garden_confirm)) },
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
                title = { Text(uiState.garden?.name ?: stringResource(R.string.garden)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        uiState.garden?.let { garden ->
                            editName = garden.name
                            editDescription = garden.description ?: ""
                            editEmoji = garden.emoji ?: ""
                            editing = true
                        }
                    }) {
                        Icon(Icons.Default.Edit, stringResource(R.string.edit))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = verdantTopAppBarColors()
            )
        },
        floatingActionButton = {
            uiState.garden?.let { garden ->
                FloatingActionButton(
                    onClick = { onCreateBed(garden.id) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.add_bed))
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
                    uiState.garden?.let { garden ->
                        item {
                            Spacer(Modifier.height(4.dp))
                            Text(garden.emoji ?: "\uD83C\uDF31", fontSize = 48.sp)
                            garden.description?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(it, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.beds), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }

                    if (uiState.beds.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.no_beds_yet),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }

                    items(uiState.beds) { bed ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onBedClick(bed.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(bed.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                bed.description?.let {
                                    Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                BedConditionChips(bed)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

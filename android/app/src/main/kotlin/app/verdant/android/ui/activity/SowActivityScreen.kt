package app.verdant.android.ui.activity

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.BatchSowRequest
import app.verdant.android.data.model.BedWithGardenResponse
import app.verdant.android.data.model.CompleteTaskPartiallyRequest
import app.verdant.android.data.model.DecrementSeedInventoryRequest
import app.verdant.android.data.model.RecordCommentRequest
import app.verdant.android.data.model.ScheduledTaskResponse
import app.verdant.android.data.model.SeedInventoryResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.faltet.FaltetDropdown
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetImagePicker
import app.verdant.android.ui.faltet.FaltetScopeToggle
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.supplies.SupplyUsageBottomSheet
import app.verdant.android.ui.theme.FaltetClay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

private const val TAG = "SowActivityScreen"

data class SowActivityState(
    val isLoading: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
    val species: List<SpeciesResponse> = emptyList(),
    val beds: List<BedWithGardenResponse> = emptyList(),
    val comments: List<String> = emptyList(),
    val seedBatches: List<SeedInventoryResponse> = emptyList(),
    val task: ScheduledTaskResponse? = null,
)

@HiltViewModel
class SowActivityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val repo: GardenRepository
) : ViewModel() {
    val taskId: Long? = savedStateHandle.get<Long>("taskId")?.takeIf { it > 0 }
    val preselectedSpeciesId: Long? = savedStateHandle.get<Long>("speciesId")?.takeIf { it > 0 }
    val preselectedBedId: Long? = savedStateHandle.get<Long>("bedId")?.takeIf { it > 0 }
    private val _uiState = MutableStateFlow(SowActivityState())
    val uiState = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val species = repo.getSpecies()
                val beds = repo.getAllBeds()
                val comments = repo.getFrequentComments().map { it.text }
                val task = taskId?.let { repo.getTask(it) }
                _uiState.value = _uiState.value.copy(species = species, beds = beds, comments = comments, task = task)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load sow activity data", e)
            }
        }
    }

    fun loadSeedBatches(speciesId: Long) {
        viewModelScope.launch {
            try {
                val lots = repo.getSeedInventory(speciesId).filter { it.quantity > 0 }
                _uiState.value = _uiState.value.copy(seedBatches = lots)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load seed batches", e)
                _uiState.value = _uiState.value.copy(seedBatches = emptyList())
            }
        }
    }

    fun sow(bedId: Long?, speciesId: Long, name: String, seedCount: Int?, notes: String?, imageBase64: String?, seedBatchId: Long?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val count = seedCount ?: 1
                repo.batchSow(
                    BatchSowRequest(
                        bedId = bedId,
                        speciesId = speciesId,
                        name = name,
                        seedCount = count,
                        notes = notes,
                        imageBase64 = imageBase64,
                    )
                )
                // Decrement seed inventory
                if (seedBatchId != null && count > 0) {
                    repo.decrementSeedInventory(seedBatchId, DecrementSeedInventoryRequest(count))
                }
                if (!notes.isNullOrBlank()) {
                    repo.recordComment(RecordCommentRequest(notes))
                }
                // Complete task partially if performing from a scheduled task
                if (taskId != null && count > 0) {
                    repo.completeTaskPartially(taskId, CompleteTaskPartiallyRequest(count, speciesId))
                }
                _uiState.value = _uiState.value.copy(isLoading = false, created = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

private fun speciesDisplayName(s: SpeciesResponse): String {
    val name = s.commonNameSv ?: s.commonName
    val variant = s.variantNameSv ?: s.variantName
    return if (variant.isNullOrBlank()) name else "$name – $variant"
}

private enum class SowDestination { TRAY, BED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SowActivityScreen(
    onBack: () -> Unit,
    onSowComplete: () -> Unit = onBack,
    viewModel: SowActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedSpecies by remember { mutableStateOf<SpeciesResponse?>(null) }
    var selectedBed by remember { mutableStateOf<BedWithGardenResponse?>(null) }
    var destination by remember {
        mutableStateOf(
            if (viewModel.preselectedBedId != null) SowDestination.BED else SowDestination.TRAY
        )
    }
    var selectedSeedBatch by remember { mutableStateOf<SeedInventoryResponse?>(null) }
    var countText by remember { mutableStateOf("") }
    var countError by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageBase64 by remember { mutableStateOf<String?>(null) }

    // Resolve preselected species/bed once species list is loaded
    LaunchedEffect(uiState.species) {
        if (selectedSpecies == null && viewModel.preselectedSpeciesId != null) {
            selectedSpecies = uiState.species.find { it.id == viewModel.preselectedSpeciesId }
        }
    }
    LaunchedEffect(uiState.beds) {
        if (selectedBed == null && viewModel.preselectedBedId != null) {
            selectedBed = uiState.beds.find { it.id == viewModel.preselectedBedId }
        }
    }

    // Pre-fill from task
    var taskPrefilled by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.task) {
        if (uiState.task != null && !taskPrefilled) {
            selectedSpecies = uiState.species.find { it.id == uiState.task!!.speciesId }
            countText = uiState.task!!.remainingCount.toString()
            taskPrefilled = true
        }
    }

    // Load seed lots when species changes
    LaunchedEffect(selectedSpecies) {
        if (selectedSpecies != null) {
            viewModel.loadSeedBatches(selectedSpecies!!.id)
            selectedSeedBatch = null
        }
    }

    var showSupplySheet by remember { mutableStateOf(false) }

    if (showSupplySheet) {
        SupplyUsageBottomSheet(
            repo = viewModel.repo,
            onDismiss = { showSupplySheet = false },
        )
    }

    if (uiState.created) {
        AlertDialog(
            onDismissRequest = { onSowComplete() },
            title = { Text("Sådd") },
            text = { Text("Vill du registrera förbrukning av jord eller krukor?") },
            confirmButton = {
                TextButton(onClick = { showSupplySheet = true }) {
                    Text("Registrera förbrukning", color = FaltetClay)
                }
            },
            dismissButton = {
                TextButton(onClick = { onSowComplete() }) { Text("Hoppa över") }
            },
        )
    }

    val canSubmit = selectedSpecies != null &&
        (destination == SowDestination.TRAY || selectedBed != null) &&
        countText.toIntOrNull()?.let { it > 0 } == true &&
        !uiState.isLoading

    val submitAction: () -> Unit = {
        viewModel.sow(
            bedId = if (destination == SowDestination.BED) selectedBed?.id else null,
            speciesId = selectedSpecies!!.id,
            name = speciesDisplayName(selectedSpecies!!),
            seedCount = countText.toIntOrNull(),
            notes = notes.ifBlank { null },
            imageBase64 = imageBase64,
            seedBatchId = selectedSeedBatch?.id,
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(uiState.created) { if (uiState.created) { /* nav handled by dialog dismiss */ } }

    FaltetScreenScaffold(
        mastheadLeft = "§ Sådd",
        mastheadCenter = "Såaktivitet",
        bottomBar = {
            FaltetFormSubmitBar(
                label = "Så",
                onClick = submitAction,
                enabled = canSubmit,
                submitting = uiState.isLoading,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                FaltetDropdown(
                    label = "Art",
                    options = uiState.species,
                    selected = selectedSpecies,
                    onSelectedChange = { selectedSpecies = it },
                    labelFor = { speciesDisplayName(it) },
                    searchable = true,
                    required = true,
                )
            }
            item {
                FaltetScopeToggle(
                    label = "Destination",
                    options = listOf(SowDestination.TRAY, SowDestination.BED),
                    selected = destination,
                    onSelectedChange = { destination = it },
                    labelFor = { if (it == SowDestination.TRAY) "Så i brätte" else "Så direkt i bädd" },
                )
            }
            if (destination == SowDestination.BED) {
                item {
                    FaltetDropdown(
                        label = "Bädd",
                        options = uiState.beds,
                        selected = selectedBed,
                        onSelectedChange = { selectedBed = it },
                        labelFor = { "${it.gardenName} · ${it.name}" },
                        searchable = true,
                        required = true,
                    )
                }
            }
            if (selectedSpecies != null) {
                val batches = uiState.seedBatches.filter { it.speciesId == selectedSpecies!!.id }
                if (batches.isNotEmpty()) {
                    item {
                        FaltetDropdown(
                            label = "Frökälla (valfri)",
                            options = batches,
                            selected = selectedSeedBatch,
                            onSelectedChange = { selectedSeedBatch = it },
                            labelFor = { "${it.speciesName} · ${it.quantity} frön" },
                            searchable = false,
                        )
                    }
                }
            }
            item {
                Field(
                    label = "Antal frön",
                    value = countText,
                    onValueChange = { countText = it.filter { c -> c.isDigit() }; countError = null },
                    keyboardType = KeyboardType.Number,
                    required = true,
                    error = countError,
                )
            }
            item {
                FaltetImagePicker(
                    label = "Foto (valfri)",
                    value = photoBitmap,
                    onValueChange = { bitmap ->
                        photoBitmap = bitmap
                        imageBase64 = bitmap?.toCompressedBase64()
                    },
                )
            }
            item {
                Field(
                    label = "Anteckningar (valfri)",
                    value = notes,
                    onValueChange = { notes = it },
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun SowActivityScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    val previewSpecies = SpeciesResponse(
        id = 1L,
        commonName = "Cosmos",
        commonNameSv = "Kosmos",
        variantName = null,
        variantNameSv = null,
        scientificName = "Cosmos bipinnatus",
        imageFrontUrl = null,
        imageBackUrl = null,
        photos = emptyList(),
        germinationTimeDaysMin = null,
        germinationTimeDaysMax = null,
        daysToHarvestMin = null,
        daysToHarvestMax = null,
        sowingDepthMm = null,
        growingPositions = emptyList(),
        soils = emptyList(),
        heightCmMin = null,
        heightCmMax = null,
        germinationRate = null,
        tags = emptyList(),
        createdAt = "",
    )
    FaltetScreenScaffold(
        mastheadLeft = "§ Sådd",
        mastheadCenter = "Såaktivitet",
        bottomBar = {
            FaltetFormSubmitBar(
                label = "Så",
                onClick = {},
                enabled = true,
                submitting = false,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                FaltetDropdown(
                    label = "Art",
                    options = listOf(previewSpecies),
                    selected = previewSpecies,
                    onSelectedChange = {},
                    labelFor = { speciesDisplayName(it) },
                    searchable = true,
                    required = true,
                )
            }
            item {
                FaltetScopeToggle(
                    label = "Destination",
                    options = listOf(SowDestination.TRAY, SowDestination.BED),
                    selected = SowDestination.TRAY,
                    onSelectedChange = {},
                    labelFor = { if (it == SowDestination.TRAY) "Så i brätte" else "Så direkt i bädd" },
                )
            }
            item {
                Field(
                    label = "Antal frön",
                    value = "50",
                    onValueChange = {},
                    keyboardType = KeyboardType.Number,
                    required = true,
                    error = null,
                )
            }
            item {
                FaltetImagePicker(
                    label = "Foto (valfri)",
                    value = null,
                    onValueChange = {},
                )
            }
            item {
                Field(
                    label = "Anteckningar (valfri)",
                    value = "",
                    onValueChange = {},
                )
            }
        }
    }
}

package app.verdant.android.ui.activity
import app.verdant.android.data.repository.SupplyRepository
import app.verdant.android.data.repository.BedRepository
import app.verdant.android.data.repository.PlantRepository
import app.verdant.android.data.repository.SeedInventoryRepository
import app.verdant.android.data.repository.SpeciesRepository
import app.verdant.android.data.repository.TaskRepository
import app.verdant.android.data.repository.TrayLocationRepository

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.BatchSowRequest
import app.verdant.android.data.model.sortedBySwedishName
import app.verdant.android.data.model.BedWithGardenResponse
import app.verdant.android.data.model.CompleteTaskPartiallyRequest
import app.verdant.android.data.model.DecrementSeedInventoryRequest
import app.verdant.android.data.model.RecordCommentRequest
import app.verdant.android.data.model.ScheduledTaskResponse
import app.verdant.android.data.model.SeedInventoryResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.ui.faltet.FaltetDatePicker
import app.verdant.android.ui.faltet.FaltetDropdown
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetImagePicker
import app.verdant.android.ui.faltet.FaltetScopeToggle
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.supplies.SupplyUsageBottomSheet
import app.verdant.android.ui.theme.FaltetAccent
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
    val trayLocations: List<app.verdant.android.data.model.TrayLocationResponse> = emptyList(),
)

@HiltViewModel
class SowActivityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val speciesRepository: SpeciesRepository,
    private val bedRepository: BedRepository,
    private val taskRepository: TaskRepository,
    private val trayLocationRepository: TrayLocationRepository,
    private val seedInventoryRepository: SeedInventoryRepository,
    private val plantRepository: PlantRepository,
    val supplyRepository: SupplyRepository,
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
                val species = speciesRepository.list().sortedBySwedishName()
                val beds = bedRepository.listAll()
                val comments = speciesRepository.frequentComments().map { it.text }
                val task = taskId?.let { taskRepository.get(it) }
                val trayLocations = runCatching { trayLocationRepository.list() }.getOrDefault(emptyList())
                _uiState.value = _uiState.value.copy(
                    species = species, beds = beds, comments = comments, task = task,
                    trayLocations = trayLocations,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load sow activity data", e)
            }
        }
    }

    fun createTrayLocation(name: String, onCreated: (app.verdant.android.data.model.TrayLocationResponse) -> Unit) {
        viewModelScope.launch {
            try {
                val created = trayLocationRepository.create(name)
                _uiState.value = _uiState.value.copy(
                    trayLocations = (_uiState.value.trayLocations + created).sortedBy { it.name }
                )
                onCreated(created)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create tray location", e)
            }
        }
    }

    fun loadSeedBatches(speciesId: Long) {
        viewModelScope.launch {
            try {
                val lots = seedInventoryRepository.list(speciesId).filter { it.quantity > 0 }
                _uiState.value = _uiState.value.copy(seedBatches = lots)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load seed batches", e)
                _uiState.value = _uiState.value.copy(seedBatches = emptyList())
            }
        }
    }

    fun sow(bedId: Long?, trayLocationId: Long?, speciesId: Long, name: String, seedCount: Int?, notes: String?, imageBase64: String?, seedBatchId: Long?, sowDate: java.time.LocalDate?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val count = seedCount ?: 1
                plantRepository.batchSow(
                    BatchSowRequest(
                        bedId = bedId,
                        trayLocationId = trayLocationId,
                        speciesId = speciesId,
                        name = name,
                        seedCount = count,
                        notes = notes,
                        imageBase64 = imageBase64,
                        plantedDate = sowDate?.toString(),
                    )
                )
                // Decrement seed inventory
                if (seedBatchId != null && count > 0) {
                    seedInventoryRepository.decrement(seedBatchId, DecrementSeedInventoryRequest(count))
                }
                if (!notes.isNullOrBlank()) {
                    speciesRepository.recordComment(RecordCommentRequest(notes))
                }
                // Complete task partially if performing from a scheduled task
                if (taskId != null && count > 0) {
                    taskRepository.completePartially(taskId, CompleteTaskPartiallyRequest(count, speciesId))
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
    onSowComplete: (gardenId: Long?) -> Unit = { onBack() },
    viewModel: SowActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedSpecies by remember { mutableStateOf<SpeciesResponse?>(null) }
    var selectedBed by remember { mutableStateOf<BedWithGardenResponse?>(null) }
    var destination by remember {
        mutableStateOf(
            if (viewModel.preselectedBedId != null) SowDestination.BED else SowDestination.TRAY
        )
    }
    var selectedTrayLocation by remember { mutableStateOf<app.verdant.android.data.model.TrayLocationResponse?>(null) }
    var showAddTrayLocation by remember { mutableStateOf(false) }
    var selectedSeedBatch by remember { mutableStateOf<SeedInventoryResponse?>(null) }
    var countText by remember { mutableStateOf("") }
    var countError by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var sowDate by remember { mutableStateOf<java.time.LocalDate?>(java.time.LocalDate.now()) }

    val isDirty = selectedSpecies != null || countText.isNotBlank() || notes.isNotBlank() ||
        imageBase64 != null || selectedBed != null
    val unsavedGuard = app.verdant.android.ui.faltet.rememberUnsavedChangesGuard(isDirty)
    unsavedGuard.InterceptBack(onBack)
    unsavedGuard.RenderConfirmDialog()

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
    LaunchedEffect(uiState.trayLocations) {
        // Auto-select the only location when there's exactly one. 0 → null, 2+ → user picks.
        if (selectedTrayLocation == null && uiState.trayLocations.size == 1) {
            selectedTrayLocation = uiState.trayLocations.first()
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
            supplyRepository = viewModel.supplyRepository,
            onDismiss = { showSupplySheet = false },
        )
    }
    if (showAddTrayLocation) {
        var newLocationName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTrayLocation = false },
            title = { Text("Ny plats") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = newLocationName,
                    onValueChange = { newLocationName = it },
                    label = { Text("Namn") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createTrayLocation(newLocationName.trim()) { created ->
                            selectedTrayLocation = created
                        }
                        showAddTrayLocation = false
                    },
                    enabled = newLocationName.trim().isNotEmpty(),
                ) { Text("Spara", color = app.verdant.android.ui.theme.FaltetAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showAddTrayLocation = false }) { Text("Avbryt") }
            },
        )
    }

    if (uiState.created) {
        val completedGardenId = if (destination == SowDestination.BED) selectedBed?.gardenId else null
        SowCompletionDialog(
            onDismiss = { onSowComplete(completedGardenId) },
            onRegisterSupplies = { showSupplySheet = true },
            onSkip = { onSowComplete(completedGardenId) },
        )
    }

    val canSubmit = selectedSpecies != null &&
        (destination == SowDestination.TRAY || selectedBed != null) &&
        // Tray destination needs a location picked when 2+ exist.
        (destination == SowDestination.BED || uiState.trayLocations.size < 2 || selectedTrayLocation != null) &&
        countText.toIntOrNull()?.let { it > 0 } == true &&
        !uiState.isLoading

    val submitAction: () -> Unit = {
        viewModel.sow(
            bedId = if (destination == SowDestination.BED) selectedBed?.id else null,
            trayLocationId = if (destination == SowDestination.TRAY) selectedTrayLocation?.id else null,
            speciesId = selectedSpecies!!.id,
            name = speciesDisplayName(selectedSpecies!!),
            seedCount = countText.toIntOrNull(),
            notes = notes.ifBlank { null },
            imageBase64 = imageBase64,
            seedBatchId = selectedSeedBatch?.id,
            sowDate = sowDate,
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(uiState.created) { if (uiState.created) { /* nav handled by dialog dismiss */ } }

    FaltetScreenScaffold(
        mastheadLeft = "",
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
                    autoOpen = viewModel.preselectedSpeciesId == null && viewModel.taskId == null,
                )
            }
            item {
                FaltetDatePicker(
                    label = "Sådatum",
                    value = sowDate,
                    onValueChange = { sowDate = it },
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
            if (destination == SowDestination.TRAY && uiState.trayLocations.size >= 2) {
                item {
                    FaltetDropdown(
                        label = "Plats",
                        options = uiState.trayLocations,
                        selected = selectedTrayLocation,
                        onSelectedChange = { selectedTrayLocation = it },
                        labelFor = { it.name },
                        searchable = false,
                        required = true,
                    )
                    androidx.compose.material3.TextButton(onClick = { showAddTrayLocation = true }) {
                        androidx.compose.material3.Text(
                            text = "+ Ny plats",
                            color = app.verdant.android.ui.theme.FaltetAccent,
                            fontSize = 12.sp,
                        )
                    }
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

@Composable
private fun SowCompletionDialog(
    onDismiss: () -> Unit,
    onRegisterSupplies: () -> Unit,
    onSkip: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .background(
                    app.verdant.android.ui.theme.FaltetCream,
                    androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                )
                .border(
                    1.dp,
                    app.verdant.android.ui.theme.FaltetInk,
                    androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                )
                .padding(24.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "SÅDD",
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.8.sp,
                color = app.verdant.android.ui.theme.FaltetForest,
            )
            Text(
                text = "Registrera förbrukning?",
                fontFamily = app.verdant.android.ui.theme.FaltetDisplay,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 26.sp,
                color = app.verdant.android.ui.theme.FaltetInk,
            )
            Text(
                text = "Lägg till jord, krukor eller annan förbrukning som användes.",
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = app.verdant.android.ui.theme.FaltetForest,
            )
            SowCompletionCard(
                icon = androidx.compose.material.icons.Icons.Default.Inventory2,
                title = "Registrera förbrukning",
                subtitle = "Dra av jord, krukor eller etiketter",
                onClick = onRegisterSupplies,
            )
            SowCompletionCard(
                icon = androidx.compose.material.icons.Icons.Default.Close,
                title = "Hoppa över",
                subtitle = "Spara utan att registrera förbrukning",
                onClick = onSkip,
            )
        }
    }
}

@Composable
private fun SowCompletionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                app.verdant.android.ui.theme.FaltetInk,
                androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .size(44.dp)
                .background(
                    FaltetAccent.copy(alpha = 0.12f),
                    androidx.compose.foundation.shape.CircleShape,
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FaltetAccent,
                modifier = androidx.compose.ui.Modifier.size(22.dp),
            )
        }
        androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.width(14.dp))
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.weight(1f),
        ) {
            Text(
                text = title,
                fontFamily = app.verdant.android.ui.theme.FaltetDisplay,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 20.sp,
                color = app.verdant.android.ui.theme.FaltetInk,
            )
            Text(
                text = subtitle,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = app.verdant.android.ui.theme.FaltetForest,
            )
        }
        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
            contentDescription = null,
            tint = FaltetAccent,
        )
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
        mastheadLeft = "",
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

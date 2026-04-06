package app.verdant.android.ui.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.*
import app.verdant.android.ui.theme.verdantTopAppBarColors
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.supplies.SupplyUsageBottomSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import android.util.Log
import javax.inject.Inject
import androidx.lifecycle.SavedStateHandle

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SowActivityScreen(
    onBack: () -> Unit,
    onSowComplete: () -> Unit = onBack,
    viewModel: SowActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedSpeciesId by remember { mutableStateOf<Long?>(viewModel.preselectedSpeciesId) }
    var selectedBedId by remember { mutableStateOf<Long?>(viewModel.preselectedBedId) }
    var sowInTray by remember { mutableStateOf(false) }
    var selectedSeedBatchId by remember { mutableStateOf<Long?>(null) }
    var seedCount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var speciesExpanded by remember { mutableStateOf(false) }
    var bedExpanded by remember { mutableStateOf(false) }
    var seedBatchExpanded by remember { mutableStateOf(false) }
    var speciesSearch by remember { mutableStateOf("") }

    // Pre-fill from task
    var taskPrefilled by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.task) {
        if (uiState.task != null && !taskPrefilled) {
            selectedSpeciesId = uiState.task!!.speciesId
            seedCount = uiState.task!!.remainingCount.toString()
            taskPrefilled = true
        }
    }

    // Load seed lots when species changes
    LaunchedEffect(selectedSpeciesId) {
        if (selectedSpeciesId != null) {
            viewModel.loadSeedBatches(selectedSpeciesId!!)
            selectedSeedBatchId = null
        }
    }

    var showSupplySheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.created) {
        // Don't auto-navigate; we show the supply usage option first
    }

    if (showSupplySheet) {
        SupplyUsageBottomSheet(
            repo = viewModel.repo,
            onDismiss = { showSupplySheet = false },
        )
    }

    if (uiState.created) {
        AlertDialog(
            onDismissRequest = { onSowComplete() },
            title = { Text(stringResource(R.string.sow)) },
            text = { Text(stringResource(R.string.record_supply_usage) + "?") },
            confirmButton = {
                TextButton(onClick = { showSupplySheet = true }) {
                    Text(stringResource(R.string.record_usage))
                }
            },
            dismissButton = {
                TextButton(onClick = { onSowComplete() }) {
                    Text(stringResource(R.string.skip))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sow)) },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(0.dp))
            // Species picker
            Text(stringResource(R.string.species_required), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            ExposedDropdownMenuBox(
                expanded = speciesExpanded,
                onExpandedChange = { speciesExpanded = it }
            ) {
                OutlinedTextField(
                    value = speciesSearch.ifBlank {
                        uiState.species.find { it.id == selectedSpeciesId }?.let { s ->
                            val name = s.commonNameSv ?: s.commonName
                            val variant = s.variantNameSv ?: s.variantName
                            if (variant.isNullOrBlank()) name else "$name \u2013 $variant"
                        } ?: ""
                    },
                    onValueChange = { speciesSearch = it; speciesExpanded = true },
                    placeholder = { Text(stringResource(R.string.search_species)) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(speciesExpanded) },
                    singleLine = true
                )
                val filtered = uiState.species.filter {
                    speciesSearch.isBlank() || it.commonName.contains(speciesSearch, ignoreCase = true) || (it.commonNameSv?.contains(speciesSearch, ignoreCase = true) == true) || (it.variantName?.contains(speciesSearch, ignoreCase = true) == true) || (it.variantNameSv?.contains(speciesSearch, ignoreCase = true) == true)
                }
                ExposedDropdownMenu(
                    expanded = speciesExpanded,
                    onDismissRequest = { speciesExpanded = false }
                ) {
                    filtered.forEach { species ->
                        DropdownMenuItem(
                            text = {
                                val name = species.commonNameSv ?: species.commonName
                                val variant = species.variantNameSv ?: species.variantName
                                Text(if (variant.isNullOrBlank()) name else "$name \u2013 $variant")
                            },
                            onClick = {
                                selectedSpeciesId = species.id
                                speciesSearch = ""
                                speciesExpanded = false
                            }
                        )
                    }
                    if (filtered.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.no_species_found), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                            onClick = {}
                        )
                    }
                }
            }

            // Seed lot picker (shown when species selected and lots available)
            if (selectedSpeciesId != null && uiState.seedBatches.isNotEmpty()) {
                Text(stringResource(R.string.seed_batch_required), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                ExposedDropdownMenuBox(
                    expanded = seedBatchExpanded,
                    onExpandedChange = { seedBatchExpanded = it }
                ) {
                    val selectedLot = uiState.seedBatches.find { it.id == selectedSeedBatchId }
                    OutlinedTextField(
                        value = selectedLot?.let {
                            buildString {
                                append(stringResource(R.string.seeds_count_format, it.quantity))
                                it.collectionDate?.let { d -> append(" (${stringResource(R.string.collected_date, d)})") }
                            }
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text(stringResource(R.string.select_seed_batch)) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(seedBatchExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = seedBatchExpanded,
                        onDismissRequest = { seedBatchExpanded = false }
                    ) {
                        uiState.seedBatches.forEach { lot ->
                            DropdownMenuItem(
                                text = {
                                    Text(buildString {
                                        append(stringResource(R.string.seeds_count_format, lot.quantity))
                                        lot.collectionDate?.let { d -> append(" (${stringResource(R.string.collected_date, d)})") }
                                        lot.expirationDate?.let { d -> append(" \u00B7 ${stringResource(R.string.expires_date, d)}") }
                                    })
                                },
                                onClick = {
                                    selectedSeedBatchId = lot.id
                                    seedBatchExpanded = false
                                }
                            )
                        }
                    }
                }
            } else if (selectedSpeciesId != null && uiState.seedBatches.isEmpty()) {
                Text(
                    stringResource(R.string.no_seed_batches),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }

            // Tray toggle (hidden when launched from a specific bed)
            if (viewModel.preselectedBedId == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.sow_in_tray), fontSize = 16.sp)
                    Switch(
                        checked = sowInTray,
                        onCheckedChange = { sowInTray = it; if (it) selectedBedId = null }
                    )
                }
            }

            // Bed picker (hidden when sowing in tray)
            if (!sowInTray) {
                Text(stringResource(R.string.bed_required), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                ExposedDropdownMenuBox(
                    expanded = bedExpanded,
                    onExpandedChange = { bedExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.beds.find { it.id == selectedBedId }?.let { "${it.gardenName} - ${it.name}" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text(stringResource(R.string.select_bed)) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(bedExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = bedExpanded,
                        onDismissRequest = { bedExpanded = false }
                    ) {
                        uiState.beds.forEach { bed ->
                            DropdownMenuItem(
                                text = { Text("${bed.gardenName} - ${bed.name}") },
                                onClick = { selectedBedId = bed.id; bedExpanded = false }
                            )
                        }
                    }
                }
            }

            // Count
            CountField(
                value = seedCount,
                onValueChange = { seedCount = it },
                label = stringResource(R.string.seed_count)
            )

            // Photo
            Text(stringResource(R.string.photo), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            PhotoPicker(
                imageUrl = null,
                onImageCaptured = { b64, _ -> imageBase64 = b64 }
            )

            // Notes
            FrequentCommentsField(
                value = notes,
                onValueChange = { notes = it },
                suggestions = uiState.comments
            )

            Spacer(Modifier.height(16.dp))
        }
        uiState.error?.let {
            app.verdant.android.ui.common.InlineErrorBanner(it)
        }
        Button(
            onClick = {
                viewModel.sow(
                    bedId = selectedBedId,
                    speciesId = selectedSpeciesId!!,
                    name = uiState.species.find { it.id == selectedSpeciesId }?.let { s ->
                        val name = s.commonNameSv ?: s.commonName
                        val variant = s.variantNameSv ?: s.variantName
                        if (variant.isNullOrBlank()) name else "$name \u2013 $variant"
                    } ?: "",
                    seedCount = seedCount.toIntOrNull(),
                    notes = notes.ifBlank { null },
                    imageBase64 = imageBase64,
                    seedBatchId = selectedSeedBatchId,
                )
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = selectedSpeciesId != null && (sowInTray || selectedBedId != null) && (seedCount.toIntOrNull() ?: 0) > 0 && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.sow))
            }
        }
        }
    }
}

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
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import androidx.lifecycle.SavedStateHandle

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
    private val repo: GardenRepository
) : ViewModel() {
    val taskId: Long? = savedStateHandle.get<Long>("taskId")?.takeIf { it > 0 }
    val preselectedSpeciesId: Long? = savedStateHandle.get<Long>("speciesId")?.takeIf { it > 0 }
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
            } catch (_: Exception) {}
        }
    }

    fun loadSeedBatches(speciesId: Long) {
        viewModelScope.launch {
            try {
                val lots = repo.getSeedInventory(speciesId).filter { it.quantity > 0 }
                _uiState.value = _uiState.value.copy(seedBatches = lots)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(seedBatches = emptyList())
            }
        }
    }

    fun sow(bedId: Long?, speciesId: Long, name: String, seedCount: Int?, notes: String?, imageBase64: String?, seedBatchId: Long?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val request = CreatePlantRequest(
                    name = name,
                    speciesId = speciesId,
                    plantedDate = LocalDate.now().toString(),
                    status = "SEEDED",
                    seedCount = seedCount,
                    survivingCount = seedCount,
                )
                val plant = if (bedId != null) {
                    repo.createPlant(bedId, request)
                } else {
                    repo.createPlantWithoutBed(request)
                }
                // Record seeded event
                repo.addPlantEvent(
                    plant.id,
                    CreatePlantEventRequest(
                        eventType = "SEEDED",
                        eventDate = LocalDate.now().toString(),
                        plantCount = seedCount,
                        notes = notes,
                        imageBase64 = imageBase64,
                    )
                )
                // Decrement seed inventory
                if (seedBatchId != null && seedCount != null && seedCount > 0) {
                    repo.decrementSeedInventory(seedBatchId, DecrementSeedInventoryRequest(seedCount))
                }
                if (!notes.isNullOrBlank()) {
                    repo.recordComment(RecordCommentRequest(notes))
                }
                // Complete task partially if performing from a scheduled task
                if (taskId != null && seedCount != null && seedCount > 0) {
                    repo.completeTaskPartially(taskId, CompleteTaskPartiallyRequest(seedCount))
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
    viewModel: SowActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedSpeciesId by remember { mutableStateOf<Long?>(viewModel.preselectedSpeciesId) }
    var selectedBedId by remember { mutableStateOf<Long?>(null) }
    var sowInTray by remember { mutableStateOf(false) }
    var selectedSeedBatchId by remember { mutableStateOf<Long?>(null) }
    var plantName by remember { mutableStateOf("") }
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

    // Auto-fill plant name from species + load seed lots
    LaunchedEffect(selectedSpeciesId) {
        if (selectedSpeciesId != null) {
            if (plantName.isBlank()) {
                val species = uiState.species.find { it.id == selectedSpeciesId }
                if (species != null) plantName = if (species.variantName.isNullOrBlank()) species.commonName else "${species.commonName} \u2013 ${species.variantName}"
            }
            viewModel.loadSeedBatches(selectedSpeciesId!!)
            selectedSeedBatchId = null
        }
    }

    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sow)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Species picker
            Text(stringResource(R.string.species_required), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            ExposedDropdownMenuBox(
                expanded = speciesExpanded,
                onExpandedChange = { speciesExpanded = it }
            ) {
                OutlinedTextField(
                    value = speciesSearch.ifBlank {
                        uiState.species.find { it.id == selectedSpeciesId }?.let { s ->
                            if (s.variantName.isNullOrBlank()) s.commonName else "${s.commonName} \u2013 ${s.variantName}"
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
                    speciesSearch.isBlank() || it.commonName.contains(speciesSearch, ignoreCase = true) || (it.variantName?.contains(speciesSearch, ignoreCase = true) == true)
                }
                ExposedDropdownMenu(
                    expanded = speciesExpanded,
                    onDismissRequest = { speciesExpanded = false }
                ) {
                    filtered.forEach { species ->
                        DropdownMenuItem(
                            text = { Text(if (species.variantName.isNullOrBlank()) species.commonName else "${species.commonName} \u2013 ${species.variantName}") },
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

            // Tray toggle
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

            // Plant name
            OutlinedTextField(
                value = plantName,
                onValueChange = { plantName = it },
                label = { Text(stringResource(R.string.plant_name)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

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

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.sow(
                        bedId = selectedBedId,
                        speciesId = selectedSpeciesId!!,
                        name = plantName,
                        seedCount = seedCount.toIntOrNull(),
                        notes = notes.ifBlank { null },
                        imageBase64 = imageBase64,
                        seedBatchId = selectedSeedBatchId,
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = selectedSpeciesId != null && (sowInTray || selectedBedId != null) && plantName.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.sow))
                }
            }

            uiState.error?.let { app.verdant.android.ui.common.InlineErrorBanner(it) }
            Spacer(Modifier.height(32.dp))
        }
    }
}

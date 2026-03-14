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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.*
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SowActivityState(
    val isLoading: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
    val species: List<SpeciesResponse> = emptyList(),
    val beds: List<BedWithGardenResponse> = emptyList(),
    val comments: List<String> = emptyList(),
)

@HiltViewModel
class SowActivityViewModel @Inject constructor(
    private val repo: GardenRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SowActivityState())
    val uiState = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val species = repo.getSpecies()
                val beds = repo.getAllBeds()
                val comments = repo.getFrequentComments().map { it.text }
                _uiState.value = _uiState.value.copy(species = species, beds = beds, comments = comments)
            } catch (_: Exception) {}
        }
    }

    fun sow(bedId: Long, speciesId: Long, name: String, seedCount: Int?, notes: String?, imageBase64: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val plant = repo.createPlant(
                    bedId,
                    CreatePlantRequest(
                        name = name,
                        speciesId = speciesId,
                        plantedDate = LocalDate.now().toString(),
                        status = "SEEDED",
                        seedCount = seedCount,
                        survivingCount = seedCount,
                    )
                )
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
                if (!notes.isNullOrBlank()) {
                    repo.recordComment(RecordCommentRequest(notes))
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

    var selectedSpeciesId by remember { mutableStateOf<Long?>(null) }
    var selectedBedId by remember { mutableStateOf<Long?>(null) }
    var plantName by remember { mutableStateOf("") }
    var seedCount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var speciesExpanded by remember { mutableStateOf(false) }
    var bedExpanded by remember { mutableStateOf(false) }
    var speciesSearch by remember { mutableStateOf("") }

    // Auto-fill plant name from species
    LaunchedEffect(selectedSpeciesId) {
        if (selectedSpeciesId != null && plantName.isBlank()) {
            val species = uiState.species.find { it.id == selectedSpeciesId }
            if (species != null) plantName = species.commonName
        }
    }

    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sow") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            Text("Species *", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            ExposedDropdownMenuBox(
                expanded = speciesExpanded,
                onExpandedChange = { speciesExpanded = it }
            ) {
                OutlinedTextField(
                    value = speciesSearch.ifBlank {
                        uiState.species.find { it.id == selectedSpeciesId }?.commonName ?: ""
                    },
                    onValueChange = { speciesSearch = it; speciesExpanded = true },
                    placeholder = { Text("Search species...") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(speciesExpanded) },
                    singleLine = true
                )
                val filtered = uiState.species.filter {
                    speciesSearch.isBlank() || it.commonName.contains(speciesSearch, ignoreCase = true)
                }
                ExposedDropdownMenu(
                    expanded = speciesExpanded,
                    onDismissRequest = { speciesExpanded = false }
                ) {
                    filtered.forEach { species ->
                        DropdownMenuItem(
                            text = { Text(species.commonName) },
                            onClick = {
                                selectedSpeciesId = species.id
                                speciesSearch = ""
                                speciesExpanded = false
                            }
                        )
                    }
                    if (filtered.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No species found", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                            onClick = {}
                        )
                    }
                }
            }

            // Bed picker
            Text("Bed *", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            ExposedDropdownMenuBox(
                expanded = bedExpanded,
                onExpandedChange = { bedExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.beds.find { it.id == selectedBedId }?.let { "${it.gardenName} - ${it.name}" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Select bed...") },
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

            // Plant name
            OutlinedTextField(
                value = plantName,
                onValueChange = { plantName = it },
                label = { Text("Plant Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Count
            CountField(
                value = seedCount,
                onValueChange = { seedCount = it },
                label = "Seed Count"
            )

            // Photo
            Text("Photo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            PhotoPicker(
                imageBase64 = imageBase64,
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
                        bedId = selectedBedId!!,
                        speciesId = selectedSpeciesId!!,
                        name = plantName,
                        seedCount = seedCount.toIntOrNull(),
                        notes = notes.ifBlank { null },
                        imageBase64 = imageBase64,
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = selectedSpeciesId != null && selectedBedId != null && plantName.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Sow")
                }
            }

            uiState.error?.let { app.verdant.android.ui.common.InlineErrorBanner(it) }
            Spacer(Modifier.height(32.dp))
        }
    }
}

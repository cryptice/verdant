package app.verdant.android.ui.activity
import app.verdant.android.ui.bed.sortedByNaturalName
import app.verdant.android.data.repository.BedRepository
import app.verdant.android.data.repository.PlantRepository
import app.verdant.android.data.repository.SpeciesRepository
import app.verdant.android.data.repository.TrayLocationRepository

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.BatchSowRequest
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.sortedBySwedishName
import app.verdant.android.ui.faltet.FaltetDatePicker
import app.verdant.android.ui.faltet.FaltetDropdown
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetScopeToggle
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.theme.FaltetAccent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "RegisterPlantsScreen"

data class RegisterPlantsState(
    val species: List<SpeciesResponse> = emptyList(),
    val trayLocations: List<app.verdant.android.data.model.TrayLocationResponse> = emptyList(),
    val beds: List<app.verdant.android.data.model.BedWithGardenResponse> = emptyList(),
    val isLoading: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class RegisterPlantsViewModel @Inject constructor(
    private val speciesRepository: SpeciesRepository,
    private val trayLocationRepository: TrayLocationRepository,
    private val bedRepository: BedRepository,
    private val plantRepository: PlantRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterPlantsState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            try {
                val species = speciesRepository.list().sortedBySwedishName()
                val locations = runCatching { trayLocationRepository.list() }.getOrDefault(emptyList())
                val beds = runCatching { bedRepository.listAll().sortedByNaturalName() }.getOrDefault(emptyList())
                _uiState.value = _uiState.value.copy(species = species, trayLocations = locations, beds = beds)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load species", e)
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

    fun register(
        species: SpeciesResponse,
        count: Int,
        seedDate: LocalDate,
        notes: String?,
        bedId: Long?,
        trayLocationId: Long?,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val name = species.variantNameSv ?: species.variantName
                    ?: species.commonNameSv ?: species.commonName
                plantRepository.batchSow(
                    BatchSowRequest(
                        speciesId = species.id,
                        bedId = bedId,
                        trayLocationId = trayLocationId,
                        name = name,
                        seedCount = count,
                        notes = notes,
                        plantedDate = seedDate.toString(),
                    )
                )
                _uiState.value = _uiState.value.copy(isLoading = false, created = true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register plants", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

/** Where the registered plants are placed. */
private enum class Placement { BED, TRAY }

private fun speciesLabel(s: SpeciesResponse): String {
    val name = s.commonNameSv ?: s.commonName
    val variant = s.variantNameSv ?: s.variantName
    return if (variant.isNullOrBlank()) name else "$name – $variant"
}

private data class AgePreset(val label: String, val days: Int)

private val AGE_PRESETS = listOf(
    AgePreset("Idag", 0),
    AgePreset("1 v", 7),
    AgePreset("2 v", 14),
    AgePreset("4 v", 28),
    AgePreset("8 v", 56),
    AgePreset("3 mån", 90),
    AgePreset("6 mån", 180),
    AgePreset("1 år", 365),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegisterPlantsScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit = onBack,
    viewModel: RegisterPlantsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedSpecies by remember { mutableStateOf<SpeciesResponse?>(null) }
    var countText by remember { mutableStateOf("1") }
    var seedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var notes by remember { mutableStateOf("") }
    var selectedTrayLocation by remember { mutableStateOf<app.verdant.android.data.model.TrayLocationResponse?>(null) }
    var selectedBed by remember { mutableStateOf<app.verdant.android.data.model.BedWithGardenResponse?>(null) }
    var showAddTrayLocation by remember { mutableStateOf(false) }
    // Where the registered plants land. Tray is the historical default;
    // bed lets the user register plants directly into a specific bed
    // without moving them later.
    var placement by remember { mutableStateOf(Placement.TRAY) }

    // Default to bed mode when there are beds but no tray locations yet.
    LaunchedEffect(uiState.beds, uiState.trayLocations) {
        if (uiState.beds.isNotEmpty() && uiState.trayLocations.isEmpty()) {
            placement = Placement.BED
        }
    }

    LaunchedEffect(uiState.trayLocations) {
        if (selectedTrayLocation == null && uiState.trayLocations.size == 1) {
            selectedTrayLocation = uiState.trayLocations.first()
        }
    }
    LaunchedEffect(uiState.beds) {
        if (selectedBed == null && uiState.beds.size == 1) {
            selectedBed = uiState.beds.first()
        }
    }

    val isDirty = selectedSpecies != null || notes.isNotBlank() ||
        (countText.toIntOrNull() ?: 0) != 1
    val unsavedGuard = app.verdant.android.ui.faltet.rememberUnsavedChangesGuard(isDirty)
    unsavedGuard.InterceptBack(onBack)
    unsavedGuard.RenderConfirmDialog()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }

    if (uiState.created) {
        AlertDialog(
            onDismissRequest = { onComplete() },
            title = { Text("Plantor registrerade") },
            text = { Text("Sådatum satt till ${seedDate ?: LocalDate.now()}.") },
            confirmButton = {
                TextButton(onClick = { onComplete() }) { Text("OK", color = FaltetAccent) }
            },
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
                ) { Text("Spara", color = FaltetAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showAddTrayLocation = false }) { Text("Avbryt") }
            },
        )
    }

    val placementValid = when (placement) {
        Placement.BED -> selectedBed != null
        Placement.TRAY -> uiState.trayLocations.size < 2 || selectedTrayLocation != null
    }
    val canSubmit = selectedSpecies != null &&
        (countText.toIntOrNull() ?: 0) > 0 &&
        seedDate != null &&
        placementValid &&
        !uiState.isLoading

    val submit: () -> Unit = {
        viewModel.register(
            species = selectedSpecies!!,
            count = countText.toInt(),
            seedDate = seedDate!!,
            notes = notes.ifBlank { null },
            bedId = if (placement == Placement.BED) selectedBed?.id else null,
            trayLocationId = if (placement == Placement.TRAY) selectedTrayLocation?.id else null,
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Registrera plantor",
        bottomBar = {
            FaltetFormSubmitBar(
                label = "Registrera",
                onClick = submit,
                enabled = canSubmit,
                submitting = uiState.isLoading,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                FaltetDropdown(
                    label = "Art",
                    options = uiState.species,
                    selected = selectedSpecies,
                    onSelectedChange = { selectedSpecies = it },
                    labelFor = { speciesLabel(it) },
                    searchable = true,
                    required = true,
                )
            }
            // Placement scope toggle — only meaningful when both kinds of
            // destinations exist. If the user has only beds (or only
            // trays) we hide the toggle and just show the relevant
            // dropdown (or nothing, when there's only one option total).
            val hasBeds = uiState.beds.isNotEmpty()
            val hasTrays = uiState.trayLocations.isNotEmpty()
            if (hasBeds && hasTrays) {
                item {
                    FaltetScopeToggle(
                        label = "Placering",
                        options = listOf(Placement.BED, Placement.TRAY),
                        selected = placement,
                        onSelectedChange = { placement = it },
                        labelFor = { if (it == Placement.BED) "Bädd" else "Bricka" },
                        required = true,
                    )
                }
            }
            if (placement == Placement.BED && hasBeds) {
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
            if (placement == Placement.TRAY && uiState.trayLocations.size >= 2) {
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
                    TextButton(onClick = { showAddTrayLocation = true }) {
                        Text("+ Ny plats", color = FaltetAccent, fontSize = 12.sp)
                    }
                }
            }
            item {
                Field(
                    label = "Antal plantor",
                    value = countText,
                    onValueChange = { countText = it.filter { c -> c.isDigit() } },
                    keyboardType = KeyboardType.Number,
                    required = true,
                )
            }
            item {
                FaltetDatePicker(
                    label = "Sådatum",
                    value = seedDate,
                    onValueChange = { seedDate = it },
                    required = true,
                )
            }
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val today = LocalDate.now()
                    AGE_PRESETS.forEach { preset ->
                        val target = today.minusDays(preset.days.toLong())
                        FilterChip(
                            selected = seedDate == target,
                            onClick = { seedDate = target },
                            label = { Text(preset.label) },
                        )
                    }
                }
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

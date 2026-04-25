package app.verdant.android.ui.activity

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.BatchSowRequest
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.faltet.FaltetDatePicker
import app.verdant.android.ui.faltet.FaltetDropdown
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
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
    val isLoading: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class RegisterPlantsViewModel @Inject constructor(
    private val repo: GardenRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterPlantsState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(species = repo.getSpecies())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load species", e)
            }
        }
    }

    fun register(species: SpeciesResponse, count: Int, seedDate: LocalDate, notes: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val name = species.variantNameSv ?: species.variantName
                    ?: species.commonNameSv ?: species.commonName
                repo.batchSow(
                    BatchSowRequest(
                        speciesId = species.id,
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
    val uiState by viewModel.uiState.collectAsState()

    var selectedSpecies by remember { mutableStateOf<SpeciesResponse?>(null) }
    var countText by remember { mutableStateOf("1") }
    var seedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var notes by remember { mutableStateOf("") }

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

    val canSubmit = selectedSpecies != null &&
        (countText.toIntOrNull() ?: 0) > 0 &&
        seedDate != null &&
        !uiState.isLoading

    val submit: () -> Unit = {
        viewModel.register(
            species = selectedSpecies!!,
            count = countText.toInt(),
            seedDate = seedDate!!,
            notes = notes.ifBlank { null },
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

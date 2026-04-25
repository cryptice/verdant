package app.verdant.android.ui.plant

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import app.verdant.android.data.model.CreatePlantRequest
import app.verdant.android.data.model.IdentifyPlantRequest
import app.verdant.android.data.model.PlantSuggestion
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.activity.toCompressedBase64
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetImagePicker
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.Field
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

private const val TAG = "CreatePlantScreen"

data class CreatePlantState(
    val isLoading: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
    val identifying: Boolean = false,
    val suggestions: List<PlantSuggestion> = emptyList(),
)

@HiltViewModel
class CreatePlantViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gardenRepository: GardenRepository
) : ViewModel() {
    private val bedId: Long = savedStateHandle.get<Long>("bedId")!!
    private val _uiState = MutableStateFlow(CreatePlantState())
    val uiState = _uiState.asStateFlow()

    fun create(name: String, species: String, seedCount: Int?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                gardenRepository.createPlant(
                    bedId,
                    CreatePlantRequest(
                        name = name,
                        seedCount = seedCount,
                        survivingCount = seedCount,
                    )
                )
                _uiState.value = _uiState.value.copy(isLoading = false, created = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun identifyPlant(imageBase64: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(identifying = true, suggestions = emptyList())
            try {
                val suggestions = gardenRepository.identifyPlant(IdentifyPlantRequest(imageBase64))
                _uiState.value = _uiState.value.copy(identifying = false, suggestions = suggestions)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(identifying = false, error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlantScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: CreatePlantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var species by remember { mutableStateOf("") }
    var seedCountText by remember { mutableStateOf("") }
    var scanBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val canSubmit = name.isNotBlank() && !uiState.isLoading

    val submitAction: () -> Unit = {
        nameError = name.isBlank()
        if (!nameError) {
            viewModel.create(name, species, seedCountText.toIntOrNull())
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(uiState.created) {
        if (uiState.created) onCreated()
    }

    // Auto-populate species from top suggestion
    LaunchedEffect(uiState.suggestions) {
        if (uiState.suggestions.isNotEmpty() && species.isBlank()) {
            val top = uiState.suggestions.first()
            species = top.species
            if (name.isBlank()) name = top.commonName
        }
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Ny planta",
        bottomBar = {
            FaltetFormSubmitBar(
                label = "Skapa",
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
                FaltetImagePicker(
                    label = "Foto (valfri)",
                    value = scanBitmap,
                    onValueChange = { bitmap ->
                        scanBitmap = bitmap
                        if (bitmap != null) {
                            val b64 = bitmap.toCompressedBase64()
                            viewModel.identifyPlant(b64)
                        }
                    },
                )
            }
            if (uiState.identifying) {
                item {
                    Text("Identifierar...")
                }
            }
            if (uiState.suggestions.isNotEmpty()) {
                item {
                    Text("Förslag:")
                }
                items(uiState.suggestions.size) { i ->
                    val s = uiState.suggestions[i]
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            species = s.species
                            if (name.isBlank()) name = s.commonName
                        },
                    ) {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Text("${s.commonName} (${s.species})")
                            Text("${(s.confidence * 100).toInt()}%")
                        }
                    }
                }
            }
            item {
                Field(
                    label = "Namn",
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    required = true,
                    error = if (nameError) "Namn krävs" else null,
                )
            }
            item {
                Field(
                    label = "Art (valfri)",
                    value = species,
                    onValueChange = { species = it },
                )
            }
            item {
                Field(
                    label = "Antal frön (valfri)",
                    value = seedCountText,
                    onValueChange = { seedCountText = it.filter { c -> c.isDigit() } },
                    keyboardType = KeyboardType.Number,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun CreatePlantScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Ny planta",
        bottomBar = {
            FaltetFormSubmitBar(
                label = "Skapa",
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
                FaltetImagePicker(
                    label = "Foto (valfri)",
                    value = null,
                    onValueChange = {},
                )
            }
            item {
                Field(
                    label = "Namn",
                    value = "Tomat",
                    onValueChange = {},
                    required = true,
                    error = null,
                )
            }
            item {
                Field(
                    label = "Art (valfri)",
                    value = "Solanum lycopersicum",
                    onValueChange = {},
                )
            }
            item {
                Field(
                    label = "Antal frön (valfri)",
                    value = "12",
                    onValueChange = {},
                    keyboardType = KeyboardType.Number,
                )
            }
        }
    }
}

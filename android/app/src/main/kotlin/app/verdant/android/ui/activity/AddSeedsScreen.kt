package app.verdant.android.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import app.verdant.android.data.model.CreateSeedInventoryRequest
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.sortedBySwedishName
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.faltet.FaltetDatePicker
import app.verdant.android.ui.faltet.FaltetDropdown
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.Field
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "AddSeedsScreen"

data class AddSeedsState(
    val isLoading: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
    val species: List<SpeciesResponse> = emptyList(),
)

@HiltViewModel
class AddSeedsViewModel @Inject constructor(
    private val repo: GardenRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddSeedsState())
    val uiState = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val species = repo.getSpecies().sortedBySwedishName()
                _uiState.value = _uiState.value.copy(species = species)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load species", e)
            }
        }
    }

    fun addSeeds(speciesId: Long, quantity: Int, collectionDate: String?, expirationDate: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repo.createSeedInventory(
                    CreateSeedInventoryRequest(
                        speciesId = speciesId,
                        quantity = quantity,
                        collectionDate = collectionDate,
                        expirationDate = expirationDate,
                    )
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSeedsScreen(
    onBack: () -> Unit,
    viewModel: AddSeedsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedSpecies by remember { mutableStateOf<SpeciesResponse?>(null) }
    var quantityText by remember { mutableStateOf("") }
    var quantityError by remember { mutableStateOf(false) }
    var collectionDate by remember { mutableStateOf<LocalDate?>(null) }
    var expirationDate by remember { mutableStateOf<LocalDate?>(null) }
    var notes by remember { mutableStateOf("") }

    val canSubmit = selectedSpecies != null &&
        quantityText.toIntOrNull()?.let { it > 0 } == true &&
        !uiState.isLoading

    val submitAction: () -> Unit = {
        val qty = quantityText.toIntOrNull()
        if (qty == null || qty <= 0) {
            quantityError = true
        } else {
            viewModel.addSeeds(
                speciesId = selectedSpecies!!.id,
                quantity = qty,
                collectionDate = collectionDate?.toString(),
                expirationDate = expirationDate?.toString(),
            )
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Lägg till frön",
        bottomBar = {
            FaltetFormSubmitBar(
                label = "Spara",
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
                Field(
                    label = "Antal frön",
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { c -> c.isDigit() }; quantityError = false },
                    keyboardType = KeyboardType.Number,
                    required = true,
                    error = if (quantityError) "Antal krävs" else null,
                )
            }
            item {
                FaltetDatePicker(
                    label = "Skördedatum (valfri)",
                    value = collectionDate,
                    onValueChange = { collectionDate = it },
                )
            }
            item {
                FaltetDatePicker(
                    label = "Utgångsdatum (valfri)",
                    value = expirationDate,
                    onValueChange = { expirationDate = it },
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
private fun AddSeedsScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    val previewSpecies = listOf(
        SpeciesResponse(
            id = 1L,
            commonName = "Dahlia",
            commonNameSv = "Dahlia",
            variantName = "Bishop of Llandaff",
            variantNameSv = null,
            scientificName = "Dahlia pinnata",
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
        ),
        SpeciesResponse(
            id = 2L,
            commonName = "Zinnia",
            commonNameSv = "Zinnia",
            variantName = null,
            variantNameSv = null,
            scientificName = "Zinnia elegans",
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
        ),
    )
    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Lägg till frön",
        bottomBar = {
            FaltetFormSubmitBar(
                label = "Spara",
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
                    options = previewSpecies,
                    selected = previewSpecies.first(),
                    onSelectedChange = {},
                    labelFor = { speciesDisplayName(it) },
                    searchable = true,
                    required = true,
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
                FaltetDatePicker(
                    label = "Skördedatum (valfri)",
                    value = LocalDate.of(2025, 9, 14),
                    onValueChange = {},
                )
            }
            item {
                FaltetDatePicker(
                    label = "Utgångsdatum (valfri)",
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

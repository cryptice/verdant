package app.verdant.android.ui.activity

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreateSeedInventoryRequest
import app.verdant.android.data.model.ProviderResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.sortedBySwedishName
import app.verdant.android.data.repository.SeedInventoryRepository
import app.verdant.android.data.repository.SpeciesRepository
import app.verdant.android.ui.faltet.FaltetDatePicker
import app.verdant.android.ui.faltet.FaltetDropdown
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.theme.FaltetForest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

private const val TAG = "AddSeedsScreen"
private val UNIT_TYPES = listOf("SEED", "PLUG", "BULB", "TUBER", "PLANT")

data class AddSeedsState(
    val isLoading: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
    val species: List<SpeciesResponse> = emptyList(),
    val providers: List<ProviderResponse> = emptyList(),
)

@HiltViewModel
class AddSeedsViewModel @Inject constructor(
    private val speciesRepository: SpeciesRepository,
    private val seedInventoryRepository: SeedInventoryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddSeedsState())
    val uiState = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val species = speciesRepository.list().sortedBySwedishName()
                val providers = runCatching { speciesRepository.providers() }.getOrDefault(emptyList()).sortedBy { it.name }
                _uiState.value = _uiState.value.copy(species = species, providers = providers)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load species", e)
            }
        }
    }

    fun addSeeds(request: CreateSeedInventoryRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                seedInventoryRepository.create(request)
                _uiState.value = _uiState.value.copy(isLoading = false, created = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = if (e is HttpException) runCatching {
                    JSONObject(e.response()?.errorBody()?.string().orEmpty()).optString("message").takeIf { it.isNotBlank() }
                }.getOrNull() else null
                _uiState.value = _uiState.value.copy(isLoading = false, error = message ?: "Kunde inte spara utsädet")
            }
        }
    }
}

private fun speciesDisplayName(s: SpeciesResponse): String {
    val name = s.commonNameSv ?: s.commonName
    val variant = s.variantNameSv ?: s.variantName
    return if (variant.isNullOrBlank()) name else "$name – $variant"
}

private fun unitTypeLabel(code: String): String = when (code) {
    "SEED" -> "Frö"
    "PLUG" -> "Plugg"
    "BULB" -> "Lök"
    "TUBER" -> "Knöl"
    "PLANT" -> "Planta"
    else -> code
}

/** A new seed batch, like the web dialog: supplier, packages and their size, unit, dates and price. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSeedsScreen(
    onBack: () -> Unit,
    viewModel: AddSeedsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var form by remember { mutableStateOf(SeedBatchForm()) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Lägg till utsäde",
        bottomBar = {
            FaltetFormSubmitBar(
                label = "Lägg till",
                onClick = { viewModel.addSeeds(form.toRequest()) },
                enabled = form.canSave && !uiState.isLoading,
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
                    selected = form.species,
                    onSelectedChange = { form = form.withSpecies(it) },
                    labelFor = ::speciesDisplayName,
                    searchable = true,
                    required = true,
                )
            }
            item {
                FaltetDropdown(
                    label = "Leverantör",
                    options = uiState.providers,
                    selected = uiState.providers.find { it.id == form.providerId },
                    onSelectedChange = { form = form.copy(providerId = it.id) },
                    labelFor = { it.name },
                    searchable = false,
                    clearLabel = "Ingen",
                    onClear = { form = form.copy(providerId = null) },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Field(label = "Per förpackning", value = form.seedsPerPackage, onValueChange = { form = form.withSeedsPerPackage(it) },
                        keyboardType = KeyboardType.Number, required = true, modifier = Modifier.weight(1f))
                    Field(label = "Förpackningar", value = form.packages, onValueChange = { v -> form = form.copy(packages = v.filter { it.isDigit() }) },
                        keyboardType = KeyboardType.Number, required = true, modifier = Modifier.weight(1f))
                }
                Text("Totalt: ${form.quantity} ${unitTypeLabel(form.unitType).lowercase()}", fontSize = 12.sp, color = FaltetForest, modifier = Modifier.padding(top = 6.dp))
            }
            item {
                FaltetDropdown(
                    label = "Enhet",
                    options = UNIT_TYPES,
                    selected = form.unitType,
                    onSelectedChange = { form = form.copy(unitType = it) },
                    labelFor = ::unitTypeLabel,
                    searchable = false,
                    required = true,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Field(label = "Per enhet (kr)", value = form.costPerSeed, onValueChange = { form = form.withCostPerSeed(it) },
                        keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                    Field(label = "Per förpackning (kr)", value = form.costPerPackage, onValueChange = { form = form.withCostPerPackage(it) },
                        keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                }
            }
            item { FaltetDatePicker(label = "Insamlad (valfri)", value = form.collectionDate, onValueChange = { form = form.copy(collectionDate = it) }) }
            item { FaltetDatePicker(label = "Utgår (valfri)", value = form.expirationDate, onValueChange = { form = form.copy(expirationDate = it) }) }
        }
    }
}

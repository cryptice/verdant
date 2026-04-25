package app.verdant.android.ui.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreateSupplyApplicationRequest
import app.verdant.android.data.model.PlantResponse
import app.verdant.android.data.model.SupplyApplicationScope
import app.verdant.android.data.model.SupplyInventoryResponse
import app.verdant.android.data.model.SupplyTypeResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.faltet.FaltetCheckbox
import app.verdant.android.ui.faltet.FaltetChecklistGroup
import app.verdant.android.ui.faltet.FaltetDropdown
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetScopeToggle
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.theme.FaltetInk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApplySupplyViewModel @Inject constructor(
    private val repo: GardenRepository,
) : ViewModel() {

    data class State(
        val bedPlants: List<PlantResponse> = emptyList(),
        val inventory: List<SupplyInventoryResponse> = emptyList(),
        val supplyTypes: List<SupplyTypeResponse> = emptyList(),
        val scope: String = SupplyApplicationScope.BED,
        val selectedPlantIds: Set<Long> = emptySet(),
        val selectedInventoryId: Long? = null,
        val quantity: String = "",
        val notes: String = "",
        val showAllCategories: Boolean = false,
        val saving: Boolean = false,
        val error: String? = null,
        val done: Boolean = false,
    )

    private val _uiState = MutableStateFlow(State())
    val uiState = _uiState.asStateFlow()

    fun load(
        bedId: Long,
        initialPlantIds: List<Long>,
        suggestedSupplyTypeId: Long?,
        suggestedQuantity: Double?,
    ) {
        viewModelScope.launch {
            val plants = runCatching { repo.getBedPlants(bedId) }.getOrDefault(emptyList())
            val inv = runCatching { repo.getSupplyInventory() }.getOrDefault(emptyList())
            val types = runCatching { repo.getSupplyTypes() }.getOrDefault(emptyList())
            val initialLot = suggestedSupplyTypeId?.let { stid ->
                inv.firstOrNull { it.supplyTypeId == stid && it.quantity > 0 }?.id
            }
            _uiState.update { s ->
                s.copy(
                    bedPlants = plants,
                    inventory = inv,
                    supplyTypes = types,
                    scope = if (initialPlantIds.isNotEmpty()) SupplyApplicationScope.PLANTS else SupplyApplicationScope.BED,
                    selectedPlantIds = initialPlantIds.toSet(),
                    selectedInventoryId = initialLot,
                    quantity = suggestedQuantity?.toString().orEmpty(),
                )
            }
        }
    }

    fun setScope(scope: String) = _uiState.update { it.copy(scope = scope) }

    fun togglePlant(plantId: Long) = _uiState.update { s ->
        val next = if (plantId in s.selectedPlantIds) s.selectedPlantIds - plantId else s.selectedPlantIds + plantId
        s.copy(selectedPlantIds = next)
    }

    fun setInventoryId(id: Long?) = _uiState.update { it.copy(selectedInventoryId = id) }
    fun setQuantity(q: String) = _uiState.update { it.copy(quantity = q) }
    fun setNotes(n: String) = _uiState.update { it.copy(notes = n) }
    fun setShowAllCategories(show: Boolean) = _uiState.update { it.copy(showAllCategories = show) }

    fun submit(bedId: Long, workflowStepId: Long?, onDone: () -> Unit) {
        val s = _uiState.value
        val q = s.quantity.toDoubleOrNull() ?: 0.0
        if (s.selectedInventoryId == null || q <= 0 ||
            (s.scope == SupplyApplicationScope.PLANTS && s.selectedPlantIds.isEmpty())
        ) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                repo.createSupplyApplication(
                    CreateSupplyApplicationRequest(
                        bedId = bedId,
                        supplyInventoryId = s.selectedInventoryId,
                        quantity = q,
                        targetScope = s.scope,
                        plantIds = if (s.scope == SupplyApplicationScope.PLANTS) s.selectedPlantIds.toList() else null,
                        workflowStepId = workflowStepId,
                        notes = s.notes.ifBlank { null },
                    )
                )
            }.onSuccess {
                _uiState.update { it.copy(saving = false, done = true) }
                onDone()
            }.onFailure { e ->
                _uiState.update { it.copy(saving = false, error = e.message ?: "Error") }
            }
        }
    }
}

private enum class Scope { BED, PLANTS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplySupplyScreen(
    bedId: Long,
    initialPlantIds: List<Long>,
    suggestedSupplyTypeId: Long?,
    suggestedQuantity: Double?,
    workflowStepId: Long?,
    onBack: () -> Unit,
    vm: ApplySupplyViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { vm.load(bedId, initialPlantIds, suggestedSupplyTypeId, suggestedQuantity) }
    val state by vm.uiState.collectAsState()

    val scope = if (state.scope == SupplyApplicationScope.BED) Scope.BED else Scope.PLANTS

    val visibleInventory = remember(state.inventory, state.supplyTypes, state.showAllCategories) {
        state.inventory.filter { inv ->
            if (inv.quantity <= 0.0) return@filter false
            if (state.showAllCategories) return@filter true
            val type = state.supplyTypes.find { it.id == inv.supplyTypeId }
            type?.category == "FERTILIZER"
        }
    }

    val selectedLot = state.inventory.find { it.id == state.selectedInventoryId }
    val quantityNum = state.quantity.toDoubleOrNull() ?: 0.0
    val quantityExceeds = selectedLot != null && quantityNum > selectedLot.quantity

    var quantityError by remember { mutableStateOf<String?>(null) }

    val canSubmit = state.selectedInventoryId != null &&
        quantityNum > 0 &&
        !quantityExceeds &&
        (state.scope == SupplyApplicationScope.BED || state.selectedPlantIds.isNotEmpty()) &&
        !state.saving

    val submitAction: () -> Unit = {
        val qty = state.quantity.toDoubleOrNull()
        quantityError = when {
            qty == null || qty <= 0 -> "Ogiltig mängd"
            qty > (selectedLot?.quantity ?: 0.0) -> "Mängd överskrider tillgängligt"
            else -> null
        }
        if (quantityError == null) {
            vm.submit(bedId, workflowStepId, onBack)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error) { state.error?.let { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(state.done) { if (state.done) onBack() }

    val activePlants = remember(state.bedPlants) { state.bedPlants.filter { it.status != "REMOVED" } }
    val selectedPlantObjects = remember(state.selectedPlantIds, activePlants) {
        activePlants.filter { it.id in state.selectedPlantIds }.toSet()
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Applicera förnödenhet",
        bottomBar = {
            FaltetFormSubmitBar(
                label = "Spara",
                onClick = submitAction,
                enabled = canSubmit,
                submitting = state.saving,
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
                FaltetScopeToggle(
                    label = "Omfattning",
                    options = listOf(Scope.BED, Scope.PLANTS),
                    selected = scope,
                    onSelectedChange = { vm.setScope(if (it == Scope.BED) SupplyApplicationScope.BED else SupplyApplicationScope.PLANTS) },
                    labelFor = { if (it == Scope.BED) "Hela bädden" else "Enskilda plantor" },
                    required = true,
                )
            }
            if (scope == Scope.PLANTS) {
                item {
                    FaltetChecklistGroup(
                        label = "Plantor",
                        options = activePlants,
                        selected = selectedPlantObjects,
                        onSelectedChange = { plants ->
                            val newIds = plants.map { it.id }.toSet()
                            val added = newIds - state.selectedPlantIds
                            val removed = state.selectedPlantIds - newIds
                            added.forEach { vm.togglePlant(it) }
                            removed.forEach { vm.togglePlant(it) }
                        },
                        labelFor = { it.name },
                        subtitleFor = { plant -> plant.status.let { "Status: ${statusLabelSv(it)}" } },
                        selectAllEnabled = true,
                        required = true,
                    )
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.setShowAllCategories(!state.showAllCategories) }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    FaltetCheckbox(
                        checked = state.showAllCategories,
                        onCheckedChange = null,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Visa alla kategorier",
                        fontSize = 14.sp,
                        color = FaltetInk,
                    )
                }
            }
            item {
                FaltetDropdown(
                    label = "Förnödenhet",
                    options = visibleInventory,
                    selected = selectedLot,
                    onSelectedChange = { vm.setInventoryId(it.id) },
                    labelFor = { "${it.supplyTypeName} · ${app.verdant.android.ui.supplies.formatQuantity(it.quantity, it.unit)}" },
                    searchable = true,
                    required = true,
                )
            }
            item {
                Field(
                    label = "Mängd",
                    value = state.quantity,
                    onValueChange = { vm.setQuantity(it); quantityError = null },
                    keyboardType = KeyboardType.Decimal,
                    required = true,
                    error = quantityError ?: if (quantityExceeds) "Mängd överskrider tillgängligt" else null,
                )
            }
            item {
                Field(
                    label = "Anteckningar (valfri)",
                    value = state.notes,
                    onValueChange = { vm.setNotes(it) },
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun ApplySupplyScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    val previewPlants = listOf(
        PlantResponse(
            id = 1L,
            name = "Cosmos #1",
            speciesId = null,
            speciesName = "Cosmos bipinnatus",
            plantedDate = null,
            status = "PLANTED_OUT",
            seedCount = null,
            survivingCount = null,
            bedId = 42L,
            createdAt = "",
            updatedAt = "",
        ),
        PlantResponse(
            id = 2L,
            name = "Zinnia #3",
            speciesId = null,
            speciesName = "Zinnia elegans",
            plantedDate = null,
            status = "GROWING",
            seedCount = null,
            survivingCount = null,
            bedId = 42L,
            createdAt = "",
            updatedAt = "",
        ),
    )
    val selectedObjects = previewPlants.toSet()
    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Applicera förnödenhet",
        bottomBar = {
            FaltetFormSubmitBar(
                label = "Spara",
                onClick = {},
                enabled = false,
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
                FaltetScopeToggle(
                    label = "Omfattning",
                    options = listOf(Scope.BED, Scope.PLANTS),
                    selected = Scope.PLANTS,
                    onSelectedChange = {},
                    labelFor = { if (it == Scope.BED) "Hela bädden" else "Enskilda plantor" },
                    required = true,
                )
            }
            item {
                FaltetChecklistGroup(
                    label = "Plantor",
                    options = previewPlants,
                    selected = selectedObjects,
                    onSelectedChange = {},
                    labelFor = { it.name },
                    subtitleFor = { plant -> "Status: ${statusLabelSv(plant.status)}" },
                    selectAllEnabled = true,
                    required = true,
                )
            }
            item {
                FaltetDropdown(
                    label = "Förnödenhet",
                    options = emptyList<SupplyInventoryResponse>(),
                    selected = null,
                    onSelectedChange = {},
                    labelFor = { it.supplyTypeName },
                    searchable = true,
                    required = true,
                )
            }
            item {
                Field(
                    label = "Mängd",
                    value = "",
                    onValueChange = {},
                    keyboardType = KeyboardType.Decimal,
                    required = true,
                    error = null,
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

private fun statusLabelSv(status: String?): String = when (status) {
    "SEEDED" -> "Sådd"
    "POTTED_UP" -> "Krukad"
    "PLANTED_OUT", "GROWING" -> "Utplanterad"
    "HARVESTED" -> "Skördad"
    null -> "—"
    else -> status
}

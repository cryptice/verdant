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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        val selectedInexhaustibleTypeId: Long? = null,
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
            // Q11: prefer the largest finite lot; fall back to inexhaustible row.
            var initialLot: Long? = null
            var initialInexhaustibleType: Long? = null
            if (suggestedSupplyTypeId != null) {
                val lot = inv.filter { it.supplyTypeId == suggestedSupplyTypeId && it.quantity > 0 }
                    .maxByOrNull { it.quantity }
                if (lot != null) {
                    initialLot = lot.id
                } else {
                    val type = types.firstOrNull { it.id == suggestedSupplyTypeId && it.inexhaustible }
                    initialInexhaustibleType = type?.id
                }
            }
            _uiState.update { s ->
                s.copy(
                    bedPlants = plants,
                    inventory = inv,
                    supplyTypes = types,
                    scope = if (initialPlantIds.isNotEmpty()) SupplyApplicationScope.PLANTS else SupplyApplicationScope.BED,
                    selectedPlantIds = initialPlantIds.toSet(),
                    selectedInventoryId = initialLot,
                    selectedInexhaustibleTypeId = initialInexhaustibleType,
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

    fun setInventoryId(id: Long?) = _uiState.update {
        it.copy(selectedInventoryId = id, selectedInexhaustibleTypeId = null)
    }
    fun setInexhaustibleTypeId(id: Long?) = _uiState.update {
        it.copy(selectedInexhaustibleTypeId = id, selectedInventoryId = null)
    }
    fun setQuantity(q: String) = _uiState.update { it.copy(quantity = q) }
    fun setNotes(n: String) = _uiState.update { it.copy(notes = n) }
    fun setShowAllCategories(show: Boolean) = _uiState.update { it.copy(showAllCategories = show) }

    fun submit(bedId: Long, workflowStepId: Long?, onDone: () -> Unit) {
        val s = _uiState.value
        val q = s.quantity.toDoubleOrNull() ?: 0.0
        val noSupply = s.selectedInventoryId == null && s.selectedInexhaustibleTypeId == null
        if (noSupply || q <= 0 ||
            (s.scope == SupplyApplicationScope.PLANTS && s.selectedPlantIds.isEmpty())
        ) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                repo.createSupplyApplication(
                    CreateSupplyApplicationRequest(
                        bedId = bedId,
                        supplyInventoryId = s.selectedInventoryId,
                        supplyTypeId = s.selectedInexhaustibleTypeId,
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

private sealed class SupplySelection {
    abstract val label: String

    data class Lot(val inv: SupplyInventoryResponse) : SupplySelection() {
        override val label: String
            get() = "${inv.supplyTypeName} · ${app.verdant.android.ui.supplies.formatQuantity(inv.quantity, inv.unit)}"
    }

    data class Inexhaustible(val type: SupplyTypeResponse) : SupplySelection() {
        override val label: String
            get() = "${type.name} · obegränsad"
    }
}

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
    val state by vm.uiState.collectAsStateWithLifecycle()

    val scope = if (state.scope == SupplyApplicationScope.BED) Scope.BED else Scope.PLANTS

    val supplyOptions = remember(state.inventory, state.supplyTypes, state.showAllCategories) {
        val typesById = state.supplyTypes.associateBy { it.id }
        val matchesCategory = { category: String ->
            state.showAllCategories || category == "FERTILIZER"
        }
        val lots = state.inventory
            .filter { it.quantity > 0.0 }
            .filter { matchesCategory(it.category) }
            .map { SupplySelection.Lot(it) }
        val seenInexhaustibleTypeIds = mutableSetOf<Long>()
        val typeRows = state.supplyTypes
            .filter { it.inexhaustible && matchesCategory(it.category) }
            .map { SupplySelection.Inexhaustible(it) }
            .also { typeRows -> typeRows.forEach { seenInexhaustibleTypeIds.add(it.type.id) } }
        // Stable ordering: by display name
        (lots + typeRows).sortedBy { it.label.lowercase() }
            .also { _ -> typesById /* silence unused warning */ }
    }

    val selectedSelection: SupplySelection? = remember(
        state.selectedInventoryId, state.selectedInexhaustibleTypeId, supplyOptions,
    ) {
        when {
            state.selectedInventoryId != null ->
                supplyOptions.firstOrNull { it is SupplySelection.Lot && it.inv.id == state.selectedInventoryId }
            state.selectedInexhaustibleTypeId != null ->
                supplyOptions.firstOrNull { it is SupplySelection.Inexhaustible && it.type.id == state.selectedInexhaustibleTypeId }
            else -> null
        }
    }

    val selectedLot = (selectedSelection as? SupplySelection.Lot)?.inv
    val quantityNum = state.quantity.toDoubleOrNull() ?: 0.0
    val quantityExceeds = selectedLot != null && quantityNum > selectedLot.quantity

    var quantityError by remember { mutableStateOf<String?>(null) }

    val canSubmit = selectedSelection != null &&
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
        mastheadCenter = "Applicera material",
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
                    label = "Material",
                    options = supplyOptions,
                    selected = selectedSelection,
                    onSelectedChange = { sel ->
                        when (sel) {
                            is SupplySelection.Lot -> vm.setInventoryId(sel.inv.id)
                            is SupplySelection.Inexhaustible -> vm.setInexhaustibleTypeId(sel.type.id)
                        }
                    },
                    labelFor = { it.label },
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
        mastheadCenter = "Applicera material",
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
                    label = "Material",
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
    "POTTED_UP" -> "Omskolad"
    "PLANTED_OUT", "GROWING" -> "Utplanterad"
    "HARVESTED" -> "Skördad"
    null -> "—"
    else -> status
}

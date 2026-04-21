package app.verdant.android.ui.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.*
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.InlineErrorBanner
import app.verdant.android.ui.theme.verdantTopAppBarColors
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

    var supplyExpanded by remember { mutableStateOf(false) }

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

    val canSubmit = state.selectedInventoryId != null &&
        quantityNum > 0 &&
        !quantityExceeds &&
        (state.scope == SupplyApplicationScope.BED || state.selectedPlantIds.isNotEmpty())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.supply_application_apply_title)) },
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
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Target scope selector
            Text(stringResource(R.string.supply_application_target_label), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    RadioButton(
                        selected = state.scope == SupplyApplicationScope.BED,
                        onClick = { vm.setScope(SupplyApplicationScope.BED) }
                    )
                    Text(stringResource(R.string.supply_application_whole_bed), fontSize = 15.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    RadioButton(
                        selected = state.scope == SupplyApplicationScope.PLANTS,
                        onClick = { vm.setScope(SupplyApplicationScope.PLANTS) }
                    )
                    Text(stringResource(R.string.supply_application_selected_plants), fontSize = 15.sp)
                }
            }

            // Plant checklist (PLANTS scope only)
            if (state.scope == SupplyApplicationScope.PLANTS) {
                Text(stringResource(R.string.supply_application_select_plants), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        val activePlants = state.bedPlants.filter { it.status != "REMOVED" }
                        if (activePlants.isEmpty()) {
                            Text(
                                stringResource(R.string.no_plants_found),
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        } else {
                            activePlants.forEach { plant ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = plant.id in state.selectedPlantIds,
                                        onCheckedChange = { vm.togglePlant(plant.id) }
                                    )
                                    Text(plant.name, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Supply dropdown
            Text(stringResource(R.string.supply_application_select_supply), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            ExposedDropdownMenuBox(
                expanded = supplyExpanded,
                onExpandedChange = { supplyExpanded = it }
            ) {
                val displayLabel = selectedLot?.let { lot ->
                    val type = state.supplyTypes.find { it.id == lot.supplyTypeId }
                    buildString {
                        append(type?.name ?: lot.supplyTypeName)
                        append(" (${lot.quantity} ${lot.unit.lowercase()} ${stringResource(R.string.supply_application_remaining)})")
                    }
                } ?: ""
                OutlinedTextField(
                    value = displayLabel,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(stringResource(R.string.supply_application_select_supply)) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(supplyExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = supplyExpanded,
                    onDismissRequest = { supplyExpanded = false }
                ) {
                    if (visibleInventory.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.no_supplies_available),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            },
                            onClick = {}
                        )
                    } else {
                        visibleInventory.forEach { inv ->
                            val type = state.supplyTypes.find { it.id == inv.supplyTypeId }
                            DropdownMenuItem(
                                text = {
                                    Text(buildString {
                                        append(type?.name ?: inv.supplyTypeName)
                                        append(" – ${inv.quantity} ${inv.unit.lowercase()}")
                                        append(" ${stringResource(R.string.supply_application_remaining)}")
                                    })
                                },
                                onClick = {
                                    vm.setInventoryId(inv.id)
                                    supplyExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Show all categories toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.showAllCategories,
                    onCheckedChange = { vm.setShowAllCategories(it) }
                )
                Text(stringResource(R.string.supply_application_show_all_categories), fontSize = 14.sp)
            }

            // Quantity input
            Text(stringResource(R.string.supply_application_quantity), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            OutlinedTextField(
                value = state.quantity,
                onValueChange = { vm.setQuantity(it) },
                placeholder = { Text("0.0") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = {
                    val unit = selectedLot?.let { lot ->
                        state.supplyTypes.find { it.id == lot.supplyTypeId }?.unit?.lowercase()
                            ?: lot.unit.lowercase()
                    } ?: ""
                    if (unit.isNotBlank()) Text(unit, fontSize = 13.sp)
                },
                isError = quantityExceeds,
                supportingText = if (quantityExceeds) {
                    { Text(stringResource(R.string.supply_application_insufficient_quantity), color = MaterialTheme.colorScheme.error) }
                } else null
            )

            // Notes
            OutlinedTextField(
                value = state.notes,
                onValueChange = { vm.setNotes(it) },
                label = { Text(stringResource(R.string.notes_optional)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            // Error
            state.error?.let { InlineErrorBanner(it) }

            // Submit
            Button(
                onClick = { vm.submit(bedId, workflowStepId, onBack) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = canSubmit && !state.saving
            ) {
                if (state.saving) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.supply_application_submit))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

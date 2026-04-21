package app.verdant.android.ui.supplies

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.SupplyInventoryResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SupplyInventoryScreen"

data class SupplyInventoryState(
    val isLoading: Boolean = true,
    val items: List<SupplyInventoryResponse> = emptyList(),
    val error: String? = null,
    val decrementingId: Long? = null,
)

@HiltViewModel
class SupplyInventoryViewModel @Inject constructor(
    private val repo: GardenRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SupplyInventoryState())
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val items = repo.getSupplyInventory()
                _uiState.value = _uiState.value.copy(isLoading = false, items = items)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load supply inventory", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun decrement(id: Long, quantity: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(decrementingId = id)
            try {
                repo.decrementSupply(id, quantity)
                refresh()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrement supply", e)
                _uiState.value = _uiState.value.copy(decrementingId = null, error = e.message)
            }
        }
    }
}

private val CATEGORY_ORDER = listOf("SOIL", "POT", "FERTILIZER", "TRAY", "LABEL", "OTHER")

private fun categoryLabel(category: String): String = when (category) {
    "SOIL" -> "Soil"
    "POT" -> "Pots"
    "FERTILIZER" -> "Fertilizer"
    "TRAY" -> "Trays"
    "LABEL" -> "Labels"
    "OTHER" -> "Other"
    else -> category
}

private fun categoryLabelSv(category: String): String = when (category) {
    "SOIL" -> "Jord"
    "POT" -> "Krukor"
    "FERTILIZER" -> "Gödsel"
    "TRAY" -> "Brätten"
    "LABEL" -> "Etiketter"
    "OTHER" -> "Övrigt"
    else -> category
}

internal fun formatQuantity(quantity: Double, unit: String): String {
    val formatted = if (quantity == quantity.toLong().toDouble()) {
        quantity.toLong().toString()
    } else {
        String.format("%.1f", quantity)
    }
    return "$formatted $unit"
}

data class SupplyTypeGroup(
    val supplyTypeId: Long,
    val name: String,
    val unit: String,
    val totalQuantity: Double,
    val batches: List<SupplyInventoryResponse>,
)

private fun categoryIcon(category: String): ImageVector = when (category) {
    "SOIL" -> Icons.Default.Grass
    "POT" -> Icons.Default.Inventory2
    "FERTILIZER" -> Icons.Default.Science
    "TRAY" -> Icons.Default.Inventory2
    "LABEL" -> Icons.Default.Label
    "OTHER" -> Icons.Default.Category
    else -> Icons.Default.Category
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplyInventoryScreen(
    onBack: () -> Unit,
    viewModel: SupplyInventoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    var useDialogBatch by remember { mutableStateOf<SupplyInventoryResponse?>(null) }
    var useAmount by remember { mutableStateOf("") }

    if (useDialogBatch != null) {
        val batch = useDialogBatch!!
        AlertDialog(
            onDismissRequest = { useDialogBatch = null; useAmount = "" },
            title = { Text("Registrera användning") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(batch.supplyTypeName)
                    Text(
                        "Tillgängligt: ${formatQuantity(batch.quantity, batch.unit)}",
                        fontSize = 13.sp,
                        color = FaltetForest,
                    )
                    OutlinedTextField(
                        value = useAmount,
                        onValueChange = { useAmount = it },
                        label = { Text("Använd antal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                val qty = useAmount.toDoubleOrNull()
                TextButton(
                    onClick = {
                        if (qty != null && qty > 0) {
                            viewModel.decrement(batch.id, qty)
                            useDialogBatch = null
                            useAmount = ""
                        }
                    },
                    enabled = qty != null && qty > 0 && qty <= batch.quantity,
                ) { Text("Använd") }
            },
            dismissButton = {
                TextButton(onClick = { useDialogBatch = null; useAmount = "" }) { Text("Avbryt") }
            },
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = "§ Inventarie",
        mastheadCenter = "Förnödenheter",
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null && uiState.items.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            uiState.items.isEmpty() -> FaltetEmptyState(
                headline = "Inga förnödenheter",
                subtitle = "Lägg till din första inventarierad förnödenhet.",
                modifier = Modifier.padding(padding),
            )
            else -> {
                val grouped = remember(uiState.items) {
                    uiState.items
                        .groupBy { it.category }
                        .toSortedMap(compareBy { CATEGORY_ORDER.indexOf(it).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE })
                        .mapValues { (_, items) ->
                            items.groupBy { it.supplyTypeId }
                                .map { (typeId, batches) ->
                                    SupplyTypeGroup(
                                        supplyTypeId = typeId,
                                        name = batches.first().supplyTypeName,
                                        unit = batches.first().unit,
                                        totalQuantity = batches.sumOf { it.quantity },
                                        batches = batches.sortedByDescending { it.quantity },
                                    )
                                }
                                .sortedBy { it.name }
                        }
                }
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    grouped.forEach { (category, typeGroups) ->
                        item(key = "header_$category") {
                            FaltetSectionHeader(label = categoryLabelSv(category))
                        }
                        items(typeGroups, key = { "type_${it.supplyTypeId}" }) { typeGroup ->
                            SupplyTypeFaltetRow(
                                group = typeGroup,
                                category = category,
                                isDecrementing = uiState.decrementingId,
                                onUseBatch = { batch ->
                                    useDialogBatch = batch
                                    useAmount = formatQuantity(batch.quantity, "").trim()
                                },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SupplyTypeFaltetRow(
    group: SupplyTypeGroup,
    category: String,
    isDecrementing: Long?,
    onUseBatch: (SupplyInventoryResponse) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        FaltetListRow(
            title = group.name,
            meta = categoryLabelSv(category),
            leading = {
                Icon(
                    imageVector = categoryIcon(category),
                    contentDescription = null,
                    tint = FaltetForest,
                    modifier = Modifier.size(18.dp),
                )
            },
            stat = {
                Text(
                    text = formatQuantity(group.totalQuantity, group.unit),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = FaltetInk,
                )
            },
            actions = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Dölj" else "Visa",
                    tint = FaltetForest,
                    modifier = Modifier.size(18.dp),
                )
            },
            onClick = { expanded = !expanded },
        )
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.fillMaxWidth()) {
                group.batches.forEachIndexed { index, batch ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                if (index < group.batches.size - 1) {
                                    drawLine(
                                        color = FaltetInkLine20,
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 1.dp.toPx(),
                                    )
                                }
                            }
                            .padding(start = 54.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = formatQuantity(batch.quantity, batch.unit),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = FaltetInk,
                            )
                            batch.notes?.let { notes ->
                                Text(text = notes, fontSize = 12.sp, color = FaltetForest)
                            }
                            batch.costSek?.let { cost ->
                                Text(
                                    text = "${cost / 100.0} kr",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = FaltetForest,
                                )
                            }
                        }
                        if (isDecrementing == batch.id) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = FaltetClay,
                            )
                        } else {
                            TextButton(onClick = { onUseBatch(batch) }) {
                                Text("Använd", color = FaltetClay, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

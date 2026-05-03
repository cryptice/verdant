package app.verdant.android.ui.supplies
import app.verdant.android.ui.faltet.BotanicalPlate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.verdant.android.data.model.SupplyInventoryResponse
import app.verdant.android.data.model.SupplyTypeResponse
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplyInventoryScreen(
    onBack: () -> Unit,
    viewModel: SupplyInventoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    var useDialogBatch by remember { mutableStateOf<SupplyInventoryResponse?>(null) }
    var useAmount by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    // When the inventory dialog asks to create a new type, it passes the
    // current category so the type form opens pre-set to it. Null when the
    // type dialog is dismissed.
    var addTypeWithCategory by remember { mutableStateOf<String?>(null) }
    // After a new type is created mid-flow, hand it back to AddSupplyDialog
    // so the user doesn't have to re-pick their brand-new entry.
    var preselectAfterCreate by remember { mutableStateOf<SupplyTypeResponse?>(null) }
    var editTypeTarget by remember { mutableStateOf<SupplyTypeResponse?>(null) }

    val loaded = uiState as? SupplyInventoryUiState.Loaded

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

    if (showAddDialog && loaded != null) {
        AddSupplyDialog(
            types = loaded.types,
            saving = loaded.saving,
            preselectedType = preselectAfterCreate,
            onDismiss = {
                showAddDialog = false
                preselectAfterCreate = null
            },
            onSubmit = { typeId, qty, costCents, notes ->
                viewModel.createInventory(typeId, qty, costCents, notes) {
                    showAddDialog = false
                    preselectAfterCreate = null
                }
            },
            onAddType = { initialCategory -> addTypeWithCategory = initialCategory },
        )
    }
    addTypeWithCategory?.let { initialCategory ->
        AddSupplyTypeDialog(
            initialCategory = initialCategory,
            onDismiss = { addTypeWithCategory = null },
            onSubmit = { name, category, unit, inexhaustible ->
                viewModel.createSupplyType(name, category, unit, inexhaustible) { created ->
                    addTypeWithCategory = null
                    if (inexhaustible) {
                        // Per spec Q9: when the new type is inexhaustible, the parent
                        // "Add inventory" dialog has nothing left to do — close it too.
                        showAddDialog = false
                    } else {
                        // Hand the new type back so AddSupplyDialog auto-
                        // selects it instead of leaving the dropdown empty.
                        preselectAfterCreate = created
                    }
                }
            },
        )
    }
    editTypeTarget?.let { type ->
        EditSupplyTypeDialog(
            type = type,
            onDismiss = { editTypeTarget = null },
            onSave = { name, category, unit, inexhaustible ->
                viewModel.updateSupplyType(type.id, name, category, unit, inexhaustible) {
                    editTypeTarget = null
                }
            },
            onDelete = {
                viewModel.deleteSupplyType(type.id) { editTypeTarget = null }
            },
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Material",
        fab = {
            FaltetFab(onClick = { showAddDialog = true }, contentDescription = "Lägg till material")
        },
        watermark = BotanicalPlate.Harvest,
) { padding ->
        when (val state = uiState) {
            is SupplyInventoryUiState.Loading -> FaltetLoadingState(Modifier.padding(padding))
            is SupplyInventoryUiState.Error -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            is SupplyInventoryUiState.Loaded -> {
                if (state.items.isEmpty() && state.types.none { it.inexhaustible }) {
                    FaltetEmptyState(
                        headline = "Inget material",
                        subtitle = "Lägg till ditt första material.",
                        modifier = Modifier.padding(padding),
                        action = {
                            Button(onClick = { showAddDialog = true }) {
                                Text("+ Lägg till material")
                            }
                        },
                    )
                } else {
                    val grouped = remember(state.items, state.types) { groupSupplies(state.items, state.types) }
                    LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                        grouped.forEach { (category, typeGroups) ->
                            item(key = "header_$category") {
                                FaltetSectionHeader(label = categoryLabelSv(category))
                            }
                            items(typeGroups, key = { "type_${it.supplyTypeId}" }) { typeGroup ->
                                SupplyTypeFaltetRow(
                                    group = typeGroup,
                                    category = category,
                                    isDecrementing = state.decrementingId,
                                    onUseBatch = { batch ->
                                        useDialogBatch = batch
                                        useAmount = formatQuantity(batch.quantity, "").trim()
                                    },
                                    onLongClick = {
                                        state.types.firstOrNull { it.id == typeGroup.supplyTypeId }
                                            ?.let { editTypeTarget = it }
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
}

private fun groupSupplies(
    items: List<SupplyInventoryResponse>,
    types: List<SupplyTypeResponse>,
): Map<String, List<SupplyTypeGroup>> {
    val byCategory: MutableMap<String, MutableList<SupplyTypeGroup>> = mutableMapOf()
    val typesById = types.associateBy { it.id }
    items.groupBy { it.supplyTypeId }.forEach { (typeId, batches) ->
        val type = typesById[typeId]
        val first = batches.first()
        byCategory.getOrPut(first.category) { mutableListOf() }.add(
            SupplyTypeGroup(
                supplyTypeId = typeId,
                name = first.supplyTypeName,
                unit = first.unit,
                totalQuantity = batches.sumOf { it.quantity },
                batches = batches.sortedByDescending { it.quantity },
                inexhaustible = type?.inexhaustible == true,
            ),
        )
    }
    val seen = items.map { it.supplyTypeId }.toSet()
    types.filter { it.inexhaustible && it.id !in seen }.forEach { type ->
        byCategory.getOrPut(type.category) { mutableListOf() }.add(
            SupplyTypeGroup(
                supplyTypeId = type.id,
                name = type.name,
                unit = type.unit,
                totalQuantity = 0.0,
                batches = emptyList(),
                inexhaustible = true,
            ),
        )
    }
    return byCategory
        .toSortedMap(compareBy { CATEGORY_ORDER.indexOf(it).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE })
        .mapValues { (_, list) -> list.sortedBy { it.name } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SupplyTypeFaltetRow(
    group: SupplyTypeGroup,
    category: String,
    isDecrementing: Long?,
    onUseBatch: (SupplyInventoryResponse) -> Unit,
    onLongClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val canExpand = group.batches.isNotEmpty()
    val statText = when {
        group.inexhaustible && group.batches.isEmpty() -> "obegränsad"
        group.inexhaustible -> "${formatQuantity(group.totalQuantity, group.unit)} · obegränsad"
        else -> formatQuantity(group.totalQuantity, group.unit)
    }
    Column(
        modifier = Modifier.combinedClickable(
            onClick = { if (canExpand) expanded = !expanded },
            onLongClick = onLongClick,
        ),
    ) {
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
                    text = statText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = if (group.inexhaustible) FaltetAccent else FaltetInk,
                )
            },
            actions = if (canExpand) {
                {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Dölj" else "Visa",
                        tint = FaltetForest,
                        modifier = Modifier.size(32.dp),
                    )
                }
            } else null,
            onClick = if (canExpand) ({ expanded = !expanded }) else null,
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
                            batch.costCents?.let { cost ->
                                Text(
                                    text = "%.2f kr".format(cost / 100.0),
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
                                color = FaltetAccent,
                            )
                        } else {
                            TextButton(onClick = { onUseBatch(batch) }) {
                                Text("Använd", color = FaltetAccent, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

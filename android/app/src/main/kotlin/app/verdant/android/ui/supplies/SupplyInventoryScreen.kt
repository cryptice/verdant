package app.verdant.android.ui.supplies

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.SupplyInventoryResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.theme.verdantTopAppBarColors
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
    "FERTILIZER" -> "G\u00f6dsel"
    "TRAY" -> "Br\u00e4tten"
    "LABEL" -> "Etiketter"
    "OTHER" -> "\u00d6vrigt"
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

    // Use dialog state
    var useDialogBatch by remember { mutableStateOf<SupplyInventoryResponse?>(null) }
    var useAmount by remember { mutableStateOf("") }

    if (useDialogBatch != null) {
        val batch = useDialogBatch!!
        AlertDialog(
            onDismissRequest = { useDialogBatch = null; useAmount = "" },
            title = { Text(stringResource(R.string.record_usage)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        batch.supplyTypeName,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        stringResource(R.string.available_format, formatQuantity(batch.quantity, batch.unit)),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    OutlinedTextField(
                        value = useAmount,
                        onValueChange = { useAmount = it },
                        label = { Text(stringResource(R.string.amount_to_use)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                ) {
                    Text(stringResource(R.string.use))
                }
            },
            dismissButton = {
                TextButton(onClick = { useDialogBatch = null; useAmount = "" }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.supplies)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = verdantTopAppBarColors(),
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            uiState.error != null && uiState.items.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    app.verdant.android.ui.common.ConnectionErrorState(onRetry = { viewModel.refresh() })
                }
            }
            uiState.items.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2, null,
                            Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.no_supplies),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
            }
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

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    grouped.forEach { (category, typeGroups) ->
                        item(key = "header_$category") {
                            Text(
                                categoryLabel(category),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }

                        items(typeGroups, key = { "type_${it.supplyTypeId}" }) { typeGroup ->
                            SupplyTypeGroupCard(
                                group = typeGroup,
                                isDecrementing = uiState.decrementingId,
                                onUseBatch = { batch ->
                                    useDialogBatch = batch
                                    useAmount = formatQuantity(batch.quantity, "").trim()
                                },
                            )
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SupplyTypeGroupCard(
    group: SupplyTypeGroup,
    isDecrementing: Long?,
    onUseBatch: (SupplyInventoryResponse) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        group.name,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                    )
                    Text(
                        formatQuantity(group.totalQuantity, group.unit),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    group.batches.forEach { batch ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    formatQuantity(batch.quantity, batch.unit),
                                    fontSize = 14.sp,
                                )
                                batch.notes?.let { notes ->
                                    Text(
                                        notes,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    )
                                }
                                batch.costSek?.let { cost ->
                                    Text(
                                        "${cost / 100.0} kr",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    )
                                }
                            }
                            if (isDecrementing == batch.id) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                FilledTonalButton(
                                    onClick = { onUseBatch(batch) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp),
                                ) {
                                    Text(stringResource(R.string.use), fontSize = 13.sp)
                                }
                            }
                        }
                        if (batch != group.batches.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

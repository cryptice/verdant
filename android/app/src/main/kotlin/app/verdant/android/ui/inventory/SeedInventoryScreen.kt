package app.verdant.android.ui.inventory
import app.verdant.android.ui.faltet.BotanicalPlate
import app.verdant.android.data.repository.SeedInventoryRepository

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.SeedInventoryResponse
import app.verdant.android.ui.common.ConnectionErrorState
import androidx.compose.material3.Button
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetSage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SeedInventoryScreen"

data class SeedInventoryState(
    val isLoading: Boolean = true,
    val items: List<SeedInventoryResponse> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class SeedInventoryViewModel @Inject constructor(
    private val seedInventoryRepository: SeedInventoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SeedInventoryState())
    val uiState = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val isColdLoad = _uiState.value.items.isEmpty()
            _uiState.value = _uiState.value.copy(isLoading = isColdLoad, error = null)
            try {
                val items = seedInventoryRepository.list()
                _uiState.value = _uiState.value.copy(isLoading = false, items = items)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            try {
                seedInventoryRepository.delete(id)
                refresh()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete seed inventory", e)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeedInventoryScreen(
    onBack: () -> Unit,
    onAddSeeds: () -> Unit = {},
    /** Opens the planner for a variety, starting from one of its batches when given. */
    onPlan: (speciesId: Long, batchId: Long?) -> Unit = { _, _ -> },
    viewModel: SeedInventoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = stringResource(R.string.seed_inventory),
        fab = { FaltetFab(onClick = onAddSeeds, contentDescription = "Lägg till frön") },
        watermark = BotanicalPlate.EmptyGarden,
) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            uiState.items.isEmpty() -> FaltetEmptyState(
                headline = "Inga frön ännu",
                subtitle = "Börja med att lägga till ditt första frö.",
                modifier = Modifier.padding(padding),
                action = {
                    Button(onClick = onAddSeeds) { Text("+ Lägg till frö") }
                },
            )
            else -> {
                // Batches are grouped under their variety, like on the web; a variety opens to show its batches.
                val groups = uiState.items.groupBy { it.speciesId }.toList()
                var expanded by rememberSaveable { mutableStateOf(setOf<Long>()) }
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    groups.forEach { (speciesId, batches) ->
                        val isExpanded = speciesId in expanded
                        item(key = "species-$speciesId") {
                            VarietyRow(
                                batches = batches,
                                expanded = isExpanded,
                                onToggle = { expanded = if (isExpanded) expanded - speciesId else expanded + speciesId },
                                onPlan = { onPlan(speciesId, null) },
                            )
                        }
                        if (isExpanded) {
                            items(batches, key = { it.id }) { item ->
                                SeedInventoryFaltetRow(
                                    item = item,
                                    title = item.providerName ?: "Parti ${batches.indexOf(item) + 1}",
                                    onDelete = { viewModel.delete(item.id) },
                                    onPlan = { onPlan(speciesId, item.id) },
                                    modifier = Modifier.padding(start = 24.dp),
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun VarietyRow(
    batches: List<SeedInventoryResponse>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPlan: () -> Unit,
) {
    val total = batches.sumOf { it.quantity }
    FaltetListRow(
        leading = null,
        title = batches.first().speciesName,
        meta = if (batches.size == 1) "1 parti" else "${batches.size} partier",
        stat = {
            Text(
                text = total.toString(),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = FaltetInk,
            )
        },
        actions = {
            if (total > 0) {
                IconButton(onClick = onPlan) {
                    Icon(Icons.Default.DateRange, contentDescription = "Planera", tint = FaltetSage, modifier = Modifier.size(18.dp))
                }
            }
            Text(if (expanded) "▼" else "▶", fontSize = 11.sp, color = FaltetForest, modifier = Modifier.padding(end = 6.dp))
        },
        onClick = onToggle,
    )
}

@Composable
private fun SeedInventoryFaltetRow(
    item: SeedInventoryResponse,
    onDelete: () -> Unit,
    onPlan: () -> Unit = {},
    title: String = item.speciesName,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Ta bort fröparti") },
            text = { Text("Ta bort ${item.quantity} frön av ${item.speciesName}?") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Ta bort")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Avbryt") }
            },
        )
    }

    val meta = buildString {
        item.collectionDate?.let { append("Skördat $it") }
        item.expirationDate?.let {
            if (isNotEmpty()) append(" · ")
            append("Utgår $it")
        }
    }.ifEmpty { null }

    FaltetListRow(
        leading = null,
        title = title,
        meta = meta,
        modifier = modifier,
        stat = {
            Text(
                text = "${item.quantity} ${item.unitType ?: "st"}",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = FaltetInk,
            )
        },
        actions = {
            if (item.quantity > 0) {
                IconButton(onClick = onPlan) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Planera",
                        tint = FaltetSage,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Ta bort",
                    tint = FaltetSage,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        onClick = null,
    )
}

@Preview(showBackground = true)
@Composable
private fun SeedInventoryScreenPreview() {
    val items = listOf(
        SeedInventoryResponse(
            id = 1L,
            speciesId = 10L,
            speciesName = "Tomat Brandywine",
            quantity = 45,
            collectionDate = "2024-08-15",
            expirationDate = "2026-08-15",
            createdAt = "2024-08-16T10:00:00",
        ),
        SeedInventoryResponse(
            id = 2L,
            speciesId = 11L,
            speciesName = "Basilika Genovese",
            quantity = 120,
            collectionDate = null,
            expirationDate = "2025-12-01",
            unitType = "frön",
            createdAt = "2024-09-01T10:00:00",
        ),
        SeedInventoryResponse(
            id = 3L,
            speciesId = 12L,
            speciesName = "Zucchini Black Beauty",
            quantity = 18,
            collectionDate = "2024-07-20",
            expirationDate = null,
            createdAt = "2024-07-21T10:00:00",
        ),
    )
    LazyColumn {
        items(items, key = { it.id }) { item ->
            SeedInventoryFaltetRow(item = item, onDelete = {})
        }
    }
}

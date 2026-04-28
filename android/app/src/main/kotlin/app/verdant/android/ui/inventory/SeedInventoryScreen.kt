package app.verdant.android.ui.inventory
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
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
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
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(uiState.items, key = { it.id }) { item ->
                    SeedInventoryFaltetRow(
                        item = item,
                        onDelete = { viewModel.delete(item.id) },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SeedInventoryFaltetRow(
    item: SeedInventoryResponse,
    onDelete: () -> Unit,
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
        title = item.speciesName,
        meta = meta,
        stat = {
            Text(
                text = "${item.quantity} ${item.unitType ?: "st"}",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = FaltetInk,
            )
        },
        actions = {
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

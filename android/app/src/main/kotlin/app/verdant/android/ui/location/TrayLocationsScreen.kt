package app.verdant.android.ui.location

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.TrayLocationResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetForest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "TrayLocationsScreen"

data class TrayLocationsState(
    val isLoading: Boolean = true,
    val locations: List<TrayLocationResponse> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class TrayLocationsViewModel @Inject constructor(
    private val repo: GardenRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrayLocationsState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                _uiState.value = TrayLocationsState(isLoading = false, locations = repo.getTrayLocations())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load tray locations", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun create(name: String) {
        viewModelScope.launch {
            try { repo.createTrayLocation(name); refresh() } catch (e: Exception) { Log.e(TAG, "create", e) }
        }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch {
            try { repo.updateTrayLocation(id, name); refresh() } catch (e: Exception) { Log.e(TAG, "rename", e) }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            try { repo.deleteTrayLocation(id); refresh() } catch (e: Exception) { Log.e(TAG, "delete", e) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrayLocationsScreen(
    onBack: () -> Unit,
    onLocationClick: (Long) -> Unit = {},
    viewModel: TrayLocationsViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TrayLocationResponse?>(null) }
    var deleting by remember { mutableStateOf<TrayLocationResponse?>(null) }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Platser",
        mastheadRight = {
            TextButton(onClick = { showAdd = true }) {
                Text("+ Ny plats", color = FaltetAccent, fontSize = 12.sp)
            }
        },
    ) { padding ->
        when {
            ui.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            ui.error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            ui.locations.isEmpty() -> FaltetEmptyState(
                headline = "Inga platser",
                subtitle = "Skapa en plats där dina brätten står.",
                modifier = Modifier.padding(padding),
                action = { Button(onClick = { showAdd = true }) { Text("+ Ny plats") } },
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(ui.locations, key = { it.id }) { loc ->
                    FaltetListRow(
                        title = loc.name,
                        meta = "${loc.activePlantCount} aktiva plantor",
                        onClick = { editing = loc },
                    )
                }
            }
        }
    }

    if (showAdd) {
        NameDialog(
            title = "Ny plats",
            initial = "",
            onDismiss = { showAdd = false },
            onConfirm = { name -> viewModel.create(name); showAdd = false },
        )
    }
    editing?.let { loc ->
        EditLocationDialog(
            loc = loc,
            onDismiss = { editing = null },
            onRename = { newName -> viewModel.rename(loc.id, newName); editing = null },
            onDelete = { editing = null; deleting = loc },
        )
    }
    deleting?.let { loc ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Ta bort plats") },
            text = {
                Text(
                    if (loc.activePlantCount > 0)
                        "${loc.activePlantCount} plantor i ${loc.name} blir utan plats. Fortsätt?"
                    else
                        "Ta bort ${loc.name}?"
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(loc.id); deleting = null }) {
                    Text("Ta bort", color = FaltetAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Avbryt", color = FaltetForest) }
            },
        )
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    val guard = app.verdant.android.ui.faltet.rememberUnsavedChangesGuard(
        isDirty = name.trim() != initial.trim() && name.trim().isNotEmpty(),
    )
    guard.RenderConfirmDialog()
    AlertDialog(
        onDismissRequest = guard.requestDismiss(onDismiss),
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Namn") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.trim().isNotEmpty(),
            ) { Text("Spara", color = FaltetAccent) }
        },
        dismissButton = {
            TextButton(onClick = guard.requestDismiss(onDismiss)) { Text("Avbryt", color = FaltetForest) }
        },
    )
}

@Composable
private fun EditLocationDialog(
    loc: TrayLocationResponse,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(loc.id) { mutableStateOf(loc.name) }
    val guard = app.verdant.android.ui.faltet.rememberUnsavedChangesGuard(
        isDirty = name.trim() != loc.name && name.trim().isNotEmpty(),
    )
    guard.RenderConfirmDialog()
    AlertDialog(
        onDismissRequest = guard.requestDismiss(onDismiss),
        title = { Text("Redigera plats") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Namn") },
                    singleLine = true,
                )
                TextButton(onClick = onDelete) {
                    Text("Ta bort plats", color = FaltetAccent)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(name.trim()) },
                enabled = name.trim().isNotEmpty() && name != loc.name,
            ) { Text("Spara", color = FaltetAccent) }
        },
        dismissButton = {
            TextButton(onClick = guard.requestDismiss(onDismiss)) { Text("Avbryt", color = FaltetForest) }
        },
    )
}

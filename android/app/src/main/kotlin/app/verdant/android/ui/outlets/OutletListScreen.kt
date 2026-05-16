package app.verdant.android.ui.outlets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreateOutletRequest
import app.verdant.android.data.model.OutletChannel
import app.verdant.android.data.model.OutletResponse
import app.verdant.android.data.model.UpdateOutletRequest
import app.verdant.android.data.repository.OutletRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetForest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OutletListState(
    val isLoading: Boolean = true,
    val items: List<OutletResponse> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class OutletListViewModel @Inject constructor(
    private val outletRepository: OutletRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OutletListState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val items = outletRepository.list()
                _uiState.value = _uiState.value.copy(isLoading = false, items = items)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun create(name: String, channel: String) {
        viewModelScope.launch {
            try {
                outletRepository.create(CreateOutletRequest(name = name, channel = channel))
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun update(id: Long, name: String, channel: String) {
        viewModelScope.launch {
            try {
                outletRepository.update(id, UpdateOutletRequest(name = name, channel = channel))
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            try {
                outletRepository.delete(id)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutletListScreen(
    onBack: () -> Unit,
    viewModel: OutletListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<OutletResponse?>(null) }
    var deleting by remember { mutableStateOf<OutletResponse?>(null) }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Försäljningskanaler",
        fab = {
            FaltetFab(
                onClick = { showCreate = true },
                contentDescription = "Lägg till försäljningskanal",
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null && uiState.items.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { ConnectionErrorState(onRetry = { viewModel.load() }) }
            uiState.items.isEmpty() -> FaltetEmptyState(
                headline = "Inga försäljningskanaler",
                subtitle = "Lägg till bondemarknad, blomsterhandel, eller en annan kanal.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(uiState.items, key = { it.id }) { outlet ->
                    FaltetListRow(
                        title = outlet.name,
                        meta = channelLabelSv(outlet.channel),
                        actions = {
                            IconButton(onClick = { deleting = outlet }) {
                                Icon(Icons.Default.Delete, contentDescription = "Ta bort", tint = FaltetClay)
                            }
                        },
                        onClick = { editing = outlet },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showCreate) {
        OutletEditDialog(
            initial = null,
            onDismiss = { showCreate = false },
            onSave = { name, channel ->
                viewModel.create(name, channel)
                showCreate = false
            },
        )
    }
    editing?.let { outlet ->
        OutletEditDialog(
            initial = outlet,
            onDismiss = { editing = null },
            onSave = { name, channel ->
                viewModel.update(outlet.id, name, channel)
                editing = null
            },
        )
    }
    deleting?.let { outlet ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Ta bort försäljningskanal") },
            text = { Text("Vill du ta bort \"${outlet.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(outlet.id)
                    deleting = null
                }) { Text("Ta bort", color = FaltetClay) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Avbryt") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OutletEditDialog(
    initial: OutletResponse?,
    onDismiss: () -> Unit,
    onSave: (name: String, channel: String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var channel by remember { mutableStateOf(initial?.channel ?: OutletChannel.FLORIST) }
    val valid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Ny försäljningskanal" else "Redigera försäljningskanal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Namn") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Kanal", fontSize = 12.sp, color = FaltetForest)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutletChannel.values.forEach { c ->
                        FilterChip(
                            selected = channel == c,
                            onClick = { channel = c },
                            label = { Text(channelLabelSv(c), fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = { onSave(name.trim(), channel) }) {
                Text(if (initial == null) "Skapa" else "Spara")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}

private fun channelLabelSv(channel: String): String = when (channel) {
    OutletChannel.FLORIST -> "Blomsterhandel"
    OutletChannel.FARMERS_MARKET -> "Bondemarknad"
    OutletChannel.CSA -> "Andelsodling"
    OutletChannel.WEDDING -> "Bröllop"
    OutletChannel.WHOLESALE -> "Grossist"
    OutletChannel.DIRECT -> "Direktförsäljning"
    OutletChannel.OTHER -> "Övrigt"
    else -> channel
}

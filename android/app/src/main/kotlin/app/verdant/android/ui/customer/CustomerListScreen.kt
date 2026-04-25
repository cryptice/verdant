package app.verdant.android.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreateCustomerRequest
import app.verdant.android.data.model.CustomerChannel
import app.verdant.android.data.model.CustomerResponse
import app.verdant.android.data.model.UpdateCustomerRequest
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerListState(
    val isLoading: Boolean = true,
    val items: List<CustomerResponse> = emptyList(),
    val error: String? = null,
    val saving: Boolean = false,
)

@HiltViewModel
class CustomerListViewModel @Inject constructor(
    private val repo: GardenRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CustomerListState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val items = repo.getCustomers()
                _uiState.value = _uiState.value.copy(isLoading = false, items = items)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun create(request: CreateCustomerRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true)
            try {
                repo.createCustomer(request)
                _uiState.value = _uiState.value.copy(saving = false)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = e.message)
            }
        }
    }

    fun update(id: Long, request: UpdateCustomerRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true)
            try {
                repo.updateCustomer(id, request)
                _uiState.value = _uiState.value.copy(saving = false)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = e.message)
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            try {
                repo.deleteCustomer(id)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    onBack: () -> Unit,
    viewModel: CustomerListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CustomerResponse?>(null) }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Kunder",
        fab = {
            FaltetFab(
                onClick = { editing = null; showDialog = true },
                contentDescription = "Ny kund",
                icon = Icons.Default.Add,
            )
        },
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
                headline = "Inga kunder",
                subtitle = "Lägg till din första kund.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(uiState.items, key = { it.id }) { customer ->
                    FaltetListRow(
                        title = customer.name,
                        meta = channelLabel(customer.channel),
                        leading = null,
                        stat = null,
                        actions = null,
                        onClick = { editing = customer; showDialog = true },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showDialog) {
        CustomerDialog(
            existing = editing,
            saving = uiState.saving,
            onDismiss = { showDialog = false },
            onSave = { name, channel, contactInfo, notes ->
                if (editing != null) {
                    viewModel.update(
                        editing!!.id,
                        UpdateCustomerRequest(
                            name = name,
                            channel = channel,
                            contactInfo = contactInfo.ifBlank { null },
                            notes = notes.ifBlank { null },
                        ),
                    )
                } else {
                    viewModel.create(
                        CreateCustomerRequest(
                            name = name,
                            channel = channel,
                            contactInfo = contactInfo.ifBlank { null },
                            notes = notes.ifBlank { null },
                        ),
                    )
                }
                showDialog = false
            },
            onDelete = editing?.let { e ->
                { viewModel.delete(e.id); showDialog = false }
            },
        )
    }
}

private fun channelLabel(channel: String): String = when (channel) {
    CustomerChannel.FLORIST -> "Blomsterhandel"
    CustomerChannel.FARMERS_MARKET -> "Bondemarknaden"
    CustomerChannel.CSA -> "Andelsodling"
    CustomerChannel.WEDDING -> "Bröllop"
    CustomerChannel.WHOLESALE -> "Grossist"
    CustomerChannel.DIRECT -> "Direktförsäljning"
    else -> "Övrigt"
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CustomerDialog(
    existing: CustomerResponse?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, channel: String, contactInfo: String, notes: String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var channel by remember { mutableStateOf(existing?.channel ?: CustomerChannel.DIRECT) }
    var contactInfo by remember { mutableStateOf(existing?.contactInfo ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Redigera kund" else "Ny kund") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Namn") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )

                Text("Kanal", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CustomerChannel.values.forEach { c ->
                        FilterChip(
                            selected = c == channel,
                            onClick = { channel = c },
                            label = { Text(channelLabel(c), fontSize = 12.sp) },
                        )
                    }
                }

                OutlinedTextField(
                    value = contactInfo,
                    onValueChange = { contactInfo = it },
                    label = { Text("Kontaktuppgifter") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Anteckningar") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Ta bort") }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && !saving,
                onClick = { onSave(name, channel, contactInfo, notes) },
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Spara")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun CustomerListScreenPreview() {
    val customers = listOf(
        CustomerResponse(1L, "Blomsterriket AB", CustomerChannel.FLORIST, "anna@blomsterriket.se", null, "2024-01-01"),
        CustomerResponse(2L, "Naturens Skafferi", CustomerChannel.FARMERS_MARKET, null, "Standplats 12", "2024-01-02"),
        CustomerResponse(3L, "Gröna Lådan", CustomerChannel.CSA, "info@gronaladan.se", null, "2024-01-03"),
    )
    Column {
        customers.forEach { customer ->
            FaltetListRow(
                title = customer.name,
                meta = channelLabel(customer.channel),
                onClick = {},
            )
        }
    }
}

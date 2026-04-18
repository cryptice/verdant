package app.verdant.android.ui.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.CreateCustomerRequest
import app.verdant.android.data.model.CustomerChannel
import app.verdant.android.data.model.CustomerResponse
import app.verdant.android.data.model.UpdateCustomerRequest
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.theme.verdantTopAppBarColors
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.customers)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = verdantTopAppBarColors(),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, stringResource(R.string.new_customer))
            }
        },
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            uiState.error != null && uiState.items.isEmpty() -> {
                ConnectionErrorState(
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(padding),
                )
            }

            uiState.items.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.no_customers),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.items, key = { it.id }) { c ->
                        CustomerCard(c, onClick = { editing = c; showDialog = true })
                    }
                }
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

@Composable
private fun CustomerCard(customer: CustomerResponse, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                channelLabel(customer.channel),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            customer.contactInfo?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun channelLabel(channel: String): String = when (channel) {
    CustomerChannel.FLORIST -> stringResource(R.string.channel_florist)
    CustomerChannel.FARMERS_MARKET -> stringResource(R.string.channel_farmers_market)
    CustomerChannel.CSA -> stringResource(R.string.channel_csa)
    CustomerChannel.WEDDING -> stringResource(R.string.channel_wedding)
    CustomerChannel.WHOLESALE -> stringResource(R.string.channel_wholesale)
    CustomerChannel.DIRECT -> stringResource(R.string.channel_direct)
    else -> stringResource(R.string.channel_other)
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
        title = { Text(stringResource(if (existing != null) R.string.edit_customer else R.string.new_customer)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.customer_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )

                Text(stringResource(R.string.customer_channel), fontWeight = FontWeight.Medium, fontSize = 13.sp)
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
                    label = { Text(stringResource(R.string.contact_info)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.delete)) }
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
                    Text(stringResource(R.string.save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

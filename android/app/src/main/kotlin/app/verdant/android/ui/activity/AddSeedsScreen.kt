package app.verdant.android.ui.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.*
import app.verdant.android.ui.theme.verdantTopAppBarColors
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.util.Log
import javax.inject.Inject

private const val TAG = "AddSeedsScreen"

data class AddSeedsState(
    val isLoading: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
    val species: List<SpeciesResponse> = emptyList(),
)

@HiltViewModel
class AddSeedsViewModel @Inject constructor(
    private val repo: GardenRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddSeedsState())
    val uiState = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val species = repo.getSpecies()
                _uiState.value = _uiState.value.copy(species = species)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load species", e)
            }
        }
    }

    fun addSeeds(speciesId: Long, quantity: Int, collectionDate: String?, expirationDate: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repo.createSeedInventory(
                    CreateSeedInventoryRequest(
                        speciesId = speciesId,
                        quantity = quantity,
                        collectionDate = collectionDate,
                        expirationDate = expirationDate,
                    )
                )
                _uiState.value = _uiState.value.copy(isLoading = false, created = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSeedsScreen(
    onBack: () -> Unit,
    viewModel: AddSeedsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedSpeciesId by remember { mutableStateOf<Long?>(null) }
    var speciesExpanded by remember { mutableStateOf(false) }
    var speciesSearch by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var collectionDate by remember { mutableStateOf<LocalDate?>(null) }
    var expirationDate by remember { mutableStateOf<LocalDate?>(null) }
    var showCollectionDatePicker by remember { mutableStateOf(false) }
    var showExpirationDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    if (showCollectionDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = collectionDate?.toEpochDay()?.times(86400000)
        )
        DatePickerDialog(
            onDismissRequest = { showCollectionDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        collectionDate = LocalDate.ofEpochDay(it / 86400000)
                    }
                    showCollectionDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showCollectionDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showExpirationDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = expirationDate?.toEpochDay()?.times(86400000)
        )
        DatePickerDialog(
            onDismissRequest = { showExpirationDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        expirationDate = LocalDate.ofEpochDay(it / 86400000)
                    }
                    showExpirationDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showExpirationDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_seeds)) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Species picker
            Text(stringResource(R.string.species_required), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            ExposedDropdownMenuBox(
                expanded = speciesExpanded,
                onExpandedChange = { speciesExpanded = it }
            ) {
                OutlinedTextField(
                    value = speciesSearch.ifBlank {
                        uiState.species.find { it.id == selectedSpeciesId }?.let { s ->
                            if (s.variantName.isNullOrBlank()) s.commonName else "${s.commonName} \u2013 ${s.variantName}"
                        } ?: ""
                    },
                    onValueChange = { speciesSearch = it; speciesExpanded = true },
                    placeholder = { Text(stringResource(R.string.search_species)) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(speciesExpanded) },
                    singleLine = true
                )
                val filtered = uiState.species.filter {
                    speciesSearch.isBlank() || it.commonName.contains(speciesSearch, ignoreCase = true) || (it.variantName?.contains(speciesSearch, ignoreCase = true) == true)
                }
                ExposedDropdownMenu(
                    expanded = speciesExpanded,
                    onDismissRequest = { speciesExpanded = false }
                ) {
                    filtered.forEach { species ->
                        DropdownMenuItem(
                            text = { Text(if (species.variantName.isNullOrBlank()) species.commonName else "${species.commonName} \u2013 ${species.variantName}") },
                            onClick = {
                                selectedSpeciesId = species.id
                                speciesSearch = ""
                                speciesExpanded = false
                            }
                        )
                    }
                    if (filtered.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.no_species_found), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                            onClick = {}
                        )
                    }
                }
            }

            // Quantity
            CountField(
                value = quantity,
                onValueChange = { quantity = it },
                label = stringResource(R.string.number_of_seeds_required)
            )

            // Collection date
            Text(stringResource(R.string.collection_date), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            OutlinedButton(
                onClick = { showCollectionDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(collectionDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: stringResource(R.string.select_date_optional))
            }

            // Expiration date
            Text(stringResource(R.string.expiration_date), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            OutlinedButton(
                onClick = { showExpirationDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(expirationDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: stringResource(R.string.select_date_optional))
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.addSeeds(
                        speciesId = selectedSpeciesId!!,
                        quantity = quantity.toInt(),
                        collectionDate = collectionDate?.toString(),
                        expirationDate = expirationDate?.toString(),
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = selectedSpeciesId != null && (quantity.toIntOrNull() ?: 0) > 0 && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.add_seeds))
                }
            }

            uiState.error?.let { app.verdant.android.ui.common.InlineErrorBanner(it) }
            Spacer(Modifier.height(32.dp))
        }
    }
}

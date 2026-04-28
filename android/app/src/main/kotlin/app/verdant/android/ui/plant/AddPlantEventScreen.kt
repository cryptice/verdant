package app.verdant.android.ui.plant
import app.verdant.android.data.repository.CustomerRepository
import app.verdant.android.data.repository.PlantRepository

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreatePlantEventRequest
import app.verdant.android.data.model.CustomerResponse
import app.verdant.android.data.model.IdentifyPlantRequest
import app.verdant.android.data.model.PlantSuggestion
import app.verdant.android.ui.activity.toCompressedBase64
import app.verdant.android.ui.faltet.FaltetChipSelector
import app.verdant.android.ui.faltet.FaltetDropdown
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetImagePicker
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "AddPlantEventScreen"

data class AddPlantEventState(
    val isLoading: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
    val identifying: Boolean = false,
    val suggestions: List<PlantSuggestion> = emptyList(),
    val customers: List<CustomerResponse> = emptyList(),
)

@HiltViewModel
class AddPlantEventViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val customerRepository: CustomerRepository,
    private val plantRepository: PlantRepository
) : ViewModel() {
    val plantId: Long = savedStateHandle.get<Long>("plantId")!!
    private val _uiState = MutableStateFlow(AddPlantEventState())
    val uiState = _uiState.asStateFlow()

    init { loadCustomers() }

    private fun loadCustomers() {
        viewModelScope.launch {
            try {
                val customers = customerRepository.list()
                _uiState.value = _uiState.value.copy(customers = customers)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load customers", e)
            }
        }
    }

    fun addEvent(request: CreatePlantEventRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                plantRepository.addEvent(plantId, request)
                _uiState.value = _uiState.value.copy(isLoading = false, created = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun identifyPlant(imageBase64: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(identifying = true, suggestions = emptyList(), error = null)
            try {
                val suggestions = plantRepository.identify(IdentifyPlantRequest(imageBase64))
                _uiState.value = _uiState.value.copy(identifying = false, suggestions = suggestions)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(identifying = false, error = "Kunde inte identifiera bilden")
            }
        }
    }
}

// toCompressedBase64 is defined in app.verdant.android.ui.activity.PhotoPicker

private val eventTypes = listOf(
    "SEEDED", "POTTED_UP", "PLANTED_OUT", "HARVESTED", "RECOVERED", "REMOVED", "NOTE",
    "BUDDING", "FIRST_BLOOM", "PEAK_BLOOM", "LAST_BLOOM",
    "LIFTED", "DIVIDED", "STORED", "PINCHED", "DISBUDDED",
)

private fun eventTypeLabelSvStr(type: String): String = when (type) {
    "SEEDED" -> "Sått"
    "POTTED_UP" -> "Skola omd"
    "PLANTED_OUT" -> "Utplanterad"
    "HARVESTED" -> "Skördad"
    "RECOVERED" -> "Återhämtad"
    "REMOVED" -> "Borttagen"
    "NOTE" -> "Anteckning"
    "BUDDING" -> "Knoppning"
    "FIRST_BLOOM" -> "Första blomma"
    "PEAK_BLOOM" -> "Full blom"
    "LAST_BLOOM" -> "Sista blomma"
    "LIFTED" -> "Uppgrävd"
    "DIVIDED" -> "Delad"
    "STORED" -> "Lagrad"
    "PINCHED" -> "Toppkörd"
    "DISBUDDED" -> "Sidoknopp borttagen"
    else -> type
}

private fun qualityLabelSvStr(grade: String): String = when (grade) {
    "A" -> "A"
    "B" -> "B"
    "C" -> "C"
    else -> grade
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlantEventScreen(
    onBack: () -> Unit,
    viewModel: AddPlantEventViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var eventType by remember { mutableStateOf<String?>(null) }
    var plantCount by remember { mutableStateOf("") }
    var weightGrams by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var stemCount by remember { mutableStateOf("") }
    var stemLengthCm by remember { mutableStateOf("") }
    var vaseLifeDays by remember { mutableStateOf("") }
    var qualityGrade by remember { mutableStateOf<String?>(null) }
    var selectedCustomer by remember { mutableStateOf<CustomerResponse?>(null) }
    var notes by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    val canSubmit = eventType != null && !uiState.isLoading

    val submitAction = {
        val suggestionsJson = if (uiState.suggestions.isNotEmpty()) {
            "[${uiState.suggestions.joinToString(",") {
                """{"species":"${it.species}","commonName":"${it.commonName}","confidence":${it.confidence}}"""
            }}]"
        } else null

        viewModel.addEvent(
            CreatePlantEventRequest(
                eventType = eventType!!,
                eventDate = LocalDate.now().toString(),
                plantCount = plantCount.toIntOrNull(),
                weightGrams = weightGrams.toDoubleOrNull(),
                quantity = quantity.toIntOrNull(),
                notes = notes.ifBlank { null },
                imageBase64 = imageBase64,
                aiSuggestions = suggestionsJson,
                stemCount = stemCount.toIntOrNull(),
                stemLengthCm = stemLengthCm.toIntOrNull(),
                vaseLifeDays = vaseLifeDays.toIntOrNull(),
                qualityGrade = qualityGrade,
                harvestDestinationId = selectedCustomer?.id,
            )
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Händelse",
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            FaltetFormSubmitBar(
                label = "Spara",
                onClick = submitAction,
                enabled = canSubmit,
                submitting = uiState.isLoading,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                FaltetImagePicker(
                    label = "Foto (valfri)",
                    value = photoBitmap,
                    onValueChange = { bitmap ->
                        photoBitmap = bitmap
                        if (bitmap != null) {
                            val b64 = bitmap.toCompressedBase64()
                            imageBase64 = b64
                            viewModel.identifyPlant(b64)
                        } else {
                            imageBase64 = null
                        }
                    },
                )
            }

            if (uiState.identifying) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    ) {
                        CircularProgressIndicator(
                            color = FaltetAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Identifierar…",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            color = FaltetForest,
                        )
                    }
                }
            }

            if (uiState.suggestions.isNotEmpty()) {
                item { FaltetSectionHeader(label = "Förslag") }
                items(uiState.suggestions, key = { it.species }) { suggestion ->
                    SuggestionRow(suggestion = suggestion)
                }
            }

            item {
                FaltetChipSelector(
                    label = "Händelsetyp",
                    options = eventTypes,
                    selected = eventType,
                    onSelectedChange = { eventType = it },
                    labelFor = { eventTypeLabelSvStr(it) },
                    required = true,
                )
            }

            if (eventType != null) {
                item {
                    Field(
                        label = "Antal plantor (valfri)",
                        value = plantCount,
                        onValueChange = { plantCount = it },
                        keyboardType = KeyboardType.Number,
                    )
                }
            }

            if (eventType == "HARVESTED") {
                item {
                    Field(
                        label = "Vikt g (valfri)",
                        value = weightGrams,
                        onValueChange = { weightGrams = it },
                        keyboardType = KeyboardType.Decimal,
                    )
                }
                item {
                    Field(
                        label = "Antal stjälkar (valfri)",
                        value = stemCount,
                        onValueChange = { stemCount = it },
                        keyboardType = KeyboardType.Number,
                    )
                }
                item {
                    Field(
                        label = "Stjälklängd cm (valfri)",
                        value = stemLengthCm,
                        onValueChange = { stemLengthCm = it },
                        keyboardType = KeyboardType.Decimal,
                    )
                }
                item {
                    Field(
                        label = "Vaslivslängd dagar (valfri)",
                        value = vaseLifeDays,
                        onValueChange = { vaseLifeDays = it },
                        keyboardType = KeyboardType.Number,
                    )
                }
                item {
                    FaltetChipSelector(
                        label = "Kvalitet (valfri)",
                        options = listOf("A", "B", "C"),
                        selected = qualityGrade,
                        onSelectedChange = { qualityGrade = it },
                        labelFor = { qualityLabelSvStr(it) },
                    )
                }
                item {
                    FaltetDropdown(
                        label = "Kund (valfri)",
                        options = uiState.customers,
                        selected = selectedCustomer,
                        onSelectedChange = { selectedCustomer = it },
                        labelFor = { it.name },
                        searchable = true,
                    )
                }
            }

            item {
                Field(
                    label = "Anteckningar (valfri)",
                    value = notes,
                    onValueChange = { notes = it },
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: PlantSuggestion) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = FaltetInkLine20,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.commonName,
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                color = FaltetInk,
            )
            Text(
                text = suggestion.species.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = FaltetForest,
            )
        }
        Text(
            text = "${(suggestion.confidence * 100).toInt()}%",
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = FaltetAccent,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun AddPlantEventScreenPreview_Harvested() {
    val uiState = AddPlantEventState(
        customers = listOf(CustomerResponse(id = 1L, name = "Blomsterbutiken", channel = "", contactInfo = null, notes = null, createdAt = "")),
    )

    var eventType by remember { mutableStateOf<String?>("HARVESTED") }
    var plantCount by remember { mutableStateOf("") }
    var weightGrams by remember { mutableStateOf("") }
    var stemCount by remember { mutableStateOf("") }
    var stemLengthCm by remember { mutableStateOf("") }
    var vaseLifeDays by remember { mutableStateOf("") }
    var qualityGrade by remember { mutableStateOf<String?>(null) }
    var selectedCustomer by remember { mutableStateOf<CustomerResponse?>(null) }
    var notes by remember { mutableStateOf("") }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val canSubmit = eventType != null

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Händelse",
        bottomBar = {
            FaltetFormSubmitBar(
                label = "Spara",
                onClick = {},
                enabled = canSubmit,
                submitting = false,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                FaltetImagePicker(
                    label = "Foto (valfri)",
                    value = photoBitmap,
                    onValueChange = { photoBitmap = it },
                )
            }
            item {
                FaltetChipSelector(
                    label = "Händelsetyp",
                    options = eventTypes,
                    selected = eventType,
                    onSelectedChange = { eventType = it },
                    labelFor = { eventTypeLabelSvStr(it) },
                    required = true,
                )
            }
            item {
                Field(
                    label = "Antal plantor (valfri)",
                    value = plantCount,
                    onValueChange = { plantCount = it },
                    keyboardType = KeyboardType.Number,
                )
            }
            item {
                Field(
                    label = "Vikt g (valfri)",
                    value = weightGrams,
                    onValueChange = { weightGrams = it },
                    keyboardType = KeyboardType.Decimal,
                )
            }
            item {
                Field(
                    label = "Antal stjälkar (valfri)",
                    value = stemCount,
                    onValueChange = { stemCount = it },
                    keyboardType = KeyboardType.Number,
                )
            }
            item {
                Field(
                    label = "Stjälklängd cm (valfri)",
                    value = stemLengthCm,
                    onValueChange = { stemLengthCm = it },
                    keyboardType = KeyboardType.Decimal,
                )
            }
            item {
                Field(
                    label = "Vaslivslängd dagar (valfri)",
                    value = vaseLifeDays,
                    onValueChange = { vaseLifeDays = it },
                    keyboardType = KeyboardType.Number,
                )
            }
            item {
                FaltetChipSelector(
                    label = "Kvalitet (valfri)",
                    options = listOf("A", "B", "C"),
                    selected = qualityGrade,
                    onSelectedChange = { qualityGrade = it },
                    labelFor = { qualityLabelSvStr(it) },
                )
            }
            item {
                FaltetDropdown(
                    label = "Kund (valfri)",
                    options = uiState.customers,
                    selected = selectedCustomer,
                    onSelectedChange = { selectedCustomer = it },
                    labelFor = { it.name },
                    searchable = true,
                )
            }
            item {
                Field(
                    label = "Anteckningar (valfri)",
                    value = notes,
                    onValueChange = { notes = it },
                )
            }
        }
    }
}

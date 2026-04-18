package app.verdant.android.ui.plant

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.ui.activity.toCompressedBase64
import app.verdant.android.ui.theme.verdantTopAppBarColors
import app.verdant.android.data.model.CreatePlantEventRequest
import app.verdant.android.data.model.CustomerResponse
import app.verdant.android.data.model.IdentifyPlantRequest
import app.verdant.android.data.model.PlantSuggestion
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import android.util.Log
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
    private val gardenRepository: GardenRepository
) : ViewModel() {
    val plantId: Long = savedStateHandle.get<Long>("plantId")!!
    private val _uiState = MutableStateFlow(AddPlantEventState())
    val uiState = _uiState.asStateFlow()

    init { loadCustomers() }

    private fun loadCustomers() {
        viewModelScope.launch {
            try {
                val customers = gardenRepository.getCustomers()
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
                gardenRepository.addPlantEvent(plantId, request)
                _uiState.value = _uiState.value.copy(isLoading = false, created = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun identifyPlant(imageBase64: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(identifying = true, suggestions = emptyList())
            try {
                val suggestions = gardenRepository.identifyPlant(IdentifyPlantRequest(imageBase64))
                _uiState.value = _uiState.value.copy(identifying = false, suggestions = suggestions)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(identifying = false, error = e.message)
            }
        }
    }
}

// toCompressedBase64 is defined in app.verdant.android.ui.activity.PhotoPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlantEventScreen(
    onBack: () -> Unit,
    viewModel: AddPlantEventViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val eventTypes = listOf(
        "SEEDED", "POTTED_UP", "PLANTED_OUT", "HARVESTED", "RECOVERED", "REMOVED", "NOTE",
        "BUDDING", "FIRST_BLOOM", "PEAK_BLOOM", "LAST_BLOOM",
        "LIFTED", "DIVIDED", "STORED", "PINCHED", "DISBUDDED",
    )
    val eventTypeLabels = mapOf(
        "SEEDED" to R.string.event_seeded,
        "POTTED_UP" to R.string.event_potted_up,
        "PLANTED_OUT" to R.string.event_planted_out,
        "HARVESTED" to R.string.event_harvested,
        "RECOVERED" to R.string.event_recovered,
        "REMOVED" to R.string.event_removed,
        "NOTE" to R.string.event_note,
        "BUDDING" to R.string.budding,
        "FIRST_BLOOM" to R.string.first_bloom,
        "PEAK_BLOOM" to R.string.peak_bloom,
        "LAST_BLOOM" to R.string.last_bloom,
        "LIFTED" to R.string.lifted,
        "DIVIDED" to R.string.divided,
        "STORED" to R.string.stored,
        "PINCHED" to R.string.pinched,
        "DISBUDDED" to R.string.disbudded,
    )
    var selectedType by remember { mutableStateOf("NOTE") }
    var plantCount by remember { mutableStateOf("") }
    var weightGrams by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var stemCount by remember { mutableStateOf("") }
    var stemLengthCm by remember { mutableStateOf("") }
    var vaseLifeDays by remember { mutableStateOf("") }
    var qualityGrade by remember { mutableStateOf<String?>(null) }
    var selectedCustomerId by remember { mutableStateOf<Long?>(null) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            imageBitmap = bitmap
            val b64 = bitmap.toCompressedBase64()
            imageBase64 = b64
            if (selectedType == "SEEDED" || selectedType == "HARVESTED") {
                viewModel.identifyPlant(b64)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(null)
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    imageBitmap = bitmap
                    val b64 = bitmap.toCompressedBase64()
                    imageBase64 = b64
                    if (selectedType == "SEEDED" || selectedType == "HARVESTED") {
                        viewModel.identifyPlant(b64)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load image from gallery", e)
            }
        }
    }

    LaunchedEffect(uiState.created) {
        if (uiState.created) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_event)) },
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
            // Event type chips -- primary types
            Text(stringResource(R.string.event_type), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            val primaryTypes = listOf("SEEDED", "POTTED_UP", "PLANTED_OUT", "HARVESTED", "RECOVERED", "REMOVED", "NOTE")
            val secondaryTypes = listOf("BUDDING", "FIRST_BLOOM", "PEAK_BLOOM", "LAST_BLOOM", "LIFTED", "DIVIDED", "STORED", "PINCHED", "DISBUDDED")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                primaryTypes.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(stringResource(eventTypeLabels[type] ?: R.string.event_note), fontSize = 11.sp) }
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                secondaryTypes.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(stringResource(eventTypeLabels[type] ?: R.string.event_note), fontSize = 11.sp) }
                    )
                }
            }

            // Conditional fields
            if (selectedType in listOf("SEEDED", "POTTED_UP", "PLANTED_OUT")) {
                OutlinedTextField(
                    value = plantCount,
                    onValueChange = { plantCount = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.plant_count)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (selectedType == "HARVESTED") {
                OutlinedTextField(
                    value = weightGrams,
                    onValueChange = { weightGrams = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.weight_grams)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.quantity_fruits)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                // Flower-specific harvest fields
                OutlinedTextField(
                    value = stemCount,
                    onValueChange = { stemCount = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.stem_count)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = stemLengthCm,
                    onValueChange = { stemLengthCm = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.stem_length)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = vaseLifeDays,
                    onValueChange = { vaseLifeDays = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.vase_life_days)) },
                    placeholder = { Text(stringResource(R.string.vase_life_days_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                // Quality grade
                Text(stringResource(R.string.quality_grade), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("A", "B", "C").forEach { grade ->
                        FilterChip(
                            selected = qualityGrade == grade,
                            onClick = { qualityGrade = if (qualityGrade == grade) null else grade },
                            label = { Text(grade) }
                        )
                    }
                }
                // Destination (customer dropdown)
                if (uiState.customers.isNotEmpty()) {
                    Text(stringResource(R.string.destination), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Box {
                        OutlinedTextField(
                            value = uiState.customers.find { it.id == selectedCustomerId }?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.destination)) },
                            modifier = Modifier.fillMaxWidth().clickable { customerDropdownExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        )
                        DropdownMenu(
                            expanded = customerDropdownExpanded,
                            onDismissRequest = { customerDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.none)) },
                                onClick = { selectedCustomerId = null; customerDropdownExpanded = false }
                            )
                            uiState.customers.forEach { customer ->
                                DropdownMenuItem(
                                    text = { Text(customer.name) },
                                    onClick = { selectedCustomerId = customer.id; customerDropdownExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            // Photo
            Text(stringResource(R.string.photo), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { launchCamera() }) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.camera))
                }
                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.gallery))
                }
            }

            imageBitmap?.let { bmp ->
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = stringResource(R.string.photo),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // AI suggestions
            if (uiState.identifying) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp))
                    Text(stringResource(R.string.identifying), fontSize = 14.sp)
                }
            }
            if (uiState.suggestions.isNotEmpty()) {
                Text(stringResource(R.string.ai_suggestions), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                uiState.suggestions.forEach { s ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${s.commonName} (${s.species})", fontWeight = FontWeight.Medium)
                            Text("Confidence: ${(s.confidence * 100).toInt()}%", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.notes_optional)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Spacer(Modifier.height(8.dp))

            // Submit
            Button(
                onClick = {
                    val suggestionsJson = if (uiState.suggestions.isNotEmpty()) {
                        "[${uiState.suggestions.joinToString(",") {
                            """{"species":"${it.species}","commonName":"${it.commonName}","confidence":${it.confidence}}"""
                        }}]"
                    } else null

                    viewModel.addEvent(
                        CreatePlantEventRequest(
                            eventType = selectedType,
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
                            harvestDestinationId = selectedCustomerId,
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.add_event))
                }
            }

            uiState.error?.let { app.verdant.android.ui.common.InlineErrorBanner(it) }
        }
    }
}

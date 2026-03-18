package app.verdant.android.ui.plant

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
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
)

@HiltViewModel
class AddPlantEventViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gardenRepository: GardenRepository
) : ViewModel() {
    val plantId: Long = savedStateHandle.get<Long>("plantId")!!
    private val _uiState = MutableStateFlow(AddPlantEventState())
    val uiState = _uiState.asStateFlow()

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

    val eventTypes = listOf("SEEDED", "POTTED_UP", "PLANTED_OUT", "HARVESTED", "RECOVERED", "REMOVED", "NOTE")
    var selectedType by remember { mutableStateOf("NOTE") }
    var plantCount by remember { mutableStateOf("") }
    var weightGrams by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
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
            // Event type chips
            Text(stringResource(R.string.event_type), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                eventTypes.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.replace("_", " "), fontSize = 11.sp) }
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

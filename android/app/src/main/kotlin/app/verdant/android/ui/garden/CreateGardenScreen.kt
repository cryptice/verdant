package app.verdant.android.ui.garden

import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.*
import app.verdant.android.data.repository.GardenRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import com.google.gson.Gson
import com.google.maps.android.compose.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.google.android.gms.maps.model.LatLng as GmsLatLng

val bedColors = listOf(
    Color(0xFF8B4513),
    Color(0xFF228B22),
    Color(0xFFDAA520),
    Color(0xFF6B8E23),
    Color(0xFFCD853F),
    Color(0xFF2E8B57),
    Color(0xFFB8860B),
    Color(0xFF556B2F),
)

data class EditableBed(
    val name: MutableState<String>,
    val description: MutableState<String>,
    val boundary: SnapshotStateList<GmsLatLng>,
    val color: Color
)

fun getCountryLatLng(): GmsLatLng {
    return when (java.util.Locale.getDefault().country) {
        "US" -> GmsLatLng(39.8, -98.5)
        "GB" -> GmsLatLng(54.0, -2.0)
        "DE" -> GmsLatLng(51.2, 10.4)
        "FR" -> GmsLatLng(46.6, 2.2)
        "SE" -> GmsLatLng(62.0, 15.0)
        "NO" -> GmsLatLng(65.0, 13.0)
        "NL" -> GmsLatLng(52.1, 5.3)
        "ES" -> GmsLatLng(40.4, -3.7)
        "IT" -> GmsLatLng(42.5, 12.5)
        "AU" -> GmsLatLng(-25.3, 133.8)
        "CA" -> GmsLatLng(56.1, -106.3)
        "JP" -> GmsLatLng(36.2, 138.3)
        else -> GmsLatLng(50.0, 10.0)
    }
}

fun List<GmsLatLng>.toJsonString(): String {
    val list = map { mapOf("lat" to it.latitude, "lng" to it.longitude) }
    return Gson().toJson(list)
}

@HiltViewModel
class CreateGardenViewModel @Inject constructor(
    private val gardenRepository: GardenRepository
) : ViewModel() {

    var currentStep by mutableIntStateOf(0)

    // Step 1: Location
    var selectedLatLng by mutableStateOf<GmsLatLng?>(null)
    var selectedAddress by mutableStateOf("")
    var searchQuery by mutableStateOf("")

    // Step 2: Layout
    var isLoadingSuggestion by mutableStateOf(false)
    var gardenName by mutableStateOf("")
    var gardenEmoji by mutableStateOf("\uD83C\uDF31")
    var gardenBoundary = mutableStateListOf<GmsLatLng>()
    var beds = mutableStateListOf<EditableBed>()

    // Result
    var createdGardenId by mutableStateOf<Long?>(null)
    var error by mutableStateOf<String?>(null)
    var isCreating by mutableStateOf(false)

    fun searchAddress(context: Context, query: String, onResult: (GmsLatLng, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val geocoder = Geocoder(context)
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(query, 1)
                if (!results.isNullOrEmpty()) {
                    val addr = results[0]
                    val latLng = GmsLatLng(addr.latitude, addr.longitude)
                    val formatted = addr.getAddressLine(0) ?: query
                    withContext(Dispatchers.Main) {
                        onResult(latLng, formatted)
                    }
                }
            } catch (_: Exception) {
                // ignore geocoding failures silently
            }
        }
    }

    fun suggestLayout() {
        val latLng = selectedLatLng ?: return
        isLoadingSuggestion = true
        error = null
        viewModelScope.launch {
            try {
                val response = gardenRepository.suggestLayout(
                    SuggestLayoutRequest(
                        latitude = latLng.latitude,
                        longitude = latLng.longitude,
                        address = selectedAddress.ifBlank { null }
                    )
                )
                gardenName = response.gardenName
                gardenBoundary.clear()
                gardenBoundary.addAll(response.boundary.map { GmsLatLng(it.lat, it.lng) })
                beds.clear()
                response.beds.forEachIndexed { index, bed ->
                    beds.add(
                        EditableBed(
                            name = mutableStateOf(bed.name),
                            description = mutableStateOf(bed.description ?: ""),
                            boundary = mutableStateListOf<GmsLatLng>().apply {
                                addAll(bed.boundary.map { GmsLatLng(it.lat, it.lng) })
                            },
                            color = bedColors[index % bedColors.size]
                        )
                    )
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to get layout suggestion"
            } finally {
                isLoadingSuggestion = false
            }
        }
    }

    fun createGarden() {
        isCreating = true
        error = null
        viewModelScope.launch {
            try {
                val request = CreateGardenWithLayoutRequest(
                    name = gardenName,
                    emoji = gardenEmoji.ifBlank { "\uD83C\uDF31" },
                    latitude = selectedLatLng?.latitude,
                    longitude = selectedLatLng?.longitude,
                    address = selectedAddress.ifBlank { null },
                    boundaryJson = if (gardenBoundary.isNotEmpty()) gardenBoundary.toList().toJsonString() else null,
                    beds = beds.map { bed ->
                        BedLayoutItem(
                            name = bed.name.value,
                            description = bed.description.value.ifBlank { null },
                            boundaryJson = if (bed.boundary.isNotEmpty()) bed.boundary.toList().toJsonString() else null
                        )
                    }
                )
                val result = gardenRepository.createGardenWithLayout(request)
                createdGardenId = result.garden.id
            } catch (e: Exception) {
                error = e.message ?: "Failed to create garden"
            } finally {
                isCreating = false
            }
        }
    }

    fun addBed() {
        val colorIndex = beds.size % bedColors.size
        beds.add(
            EditableBed(
                name = mutableStateOf("New Bed"),
                description = mutableStateOf(""),
                boundary = mutableStateListOf(),
                color = bedColors[colorIndex]
            )
        )
    }

    fun removeBed(index: Int) {
        if (index in beds.indices) {
            beds.removeAt(index)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGardenScreen(
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
    viewModel: CreateGardenViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(viewModel.createdGardenId) {
        viewModel.createdGardenId?.let { onCreated(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (viewModel.currentStep) {
                            0 -> "Pick Location"
                            1 -> "Design Garden"
                            else -> "New Garden"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.currentStep > 0) {
                            viewModel.currentStep--
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (viewModel.currentStep) {
            0 -> LocationPickerStep(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                viewModel = viewModel,
                context = context
            )
            1 -> LayoutEditorStep(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun LocationPickerStep(
    modifier: Modifier,
    viewModel: CreateGardenViewModel,
    context: Context
) {
    val initialPosition = getCountryLatLng()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 5f)
    }

    Column(modifier = modifier) {
        // Search bar
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.searchQuery = it },
            label = { Text("Search address") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                IconButton(onClick = {
                    if (viewModel.searchQuery.isNotBlank()) {
                        viewModel.searchAddress(context, viewModel.searchQuery) { latLng, address ->
                            viewModel.selectedLatLng = latLng
                            viewModel.selectedAddress = address
                            viewModel.searchQuery = address
                        }
                    }
                }) {
                    Icon(Icons.Default.Search, "Search")
                }
            },
            singleLine = true
        )

        // Animate camera when location is selected
        LaunchedEffect(viewModel.selectedLatLng) {
            viewModel.selectedLatLng?.let { latLng ->
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(latLng, 18f),
                    durationMs = 1000
                )
            }
        }

        // Map
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    viewModel.selectedLatLng = latLng
                    viewModel.selectedAddress = ""
                    // Reverse geocode tapped location
                    viewModel.searchAddress(context, "${latLng.latitude},${latLng.longitude}") { _, address ->
                        viewModel.selectedAddress = address
                        viewModel.searchQuery = address
                    }
                }
            ) {
                viewModel.selectedLatLng?.let { latLng ->
                    Marker(
                        state = rememberMarkerState(position = latLng),
                        title = "Selected Location"
                    )
                }
            }
        }

        // Selected address display
        if (viewModel.selectedAddress.isNotBlank()) {
            Text(
                text = viewModel.selectedAddress,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                maxLines = 2
            )
        }

        // Next button
        Button(
            onClick = {
                viewModel.currentStep = 1
                viewModel.suggestLayout()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = viewModel.selectedLatLng != null
        ) {
            Text("Next")
        }
    }
}

@Composable
private fun LayoutEditorStep(
    modifier: Modifier,
    viewModel: CreateGardenViewModel
) {
    if (viewModel.isLoadingSuggestion) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(
                    "AI is designing your garden...",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        return
    }

    val cameraPositionState = rememberCameraPositionState()

    // Fit camera to garden boundary
    LaunchedEffect(viewModel.gardenBoundary.toList()) {
        if (viewModel.gardenBoundary.size >= 2) {
            val boundsBuilder = LatLngBounds.builder()
            viewModel.gardenBoundary.forEach { boundsBuilder.include(it) }
            viewModel.beds.forEach { bed ->
                bed.boundary.forEach { boundsBuilder.include(it) }
            }
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100),
                durationMs = 1000
            )
        }
    }

    // Track garden boundary marker states
    val gardenMarkerStates = remember(viewModel.gardenBoundary.size) {
        viewModel.gardenBoundary.mapIndexed { index, pos ->
            index to MarkerState(position = pos)
        }
    }

    // Track bed marker states
    val bedMarkerStatesMap = remember(viewModel.beds.size, viewModel.beds.map { it.boundary.size }) {
        viewModel.beds.mapIndexed { bedIndex, bed ->
            bedIndex to bed.boundary.mapIndexed { vertexIndex, pos ->
                vertexIndex to MarkerState(position = pos)
            }
        }.toMap()
    }

    // Sync garden marker drags back to boundary
    gardenMarkerStates.forEach { (index, state) ->
        LaunchedEffect(state.position) {
            if (index < viewModel.gardenBoundary.size) {
                viewModel.gardenBoundary[index] = state.position
            }
        }
    }

    // Sync bed marker drags back to boundaries
    bedMarkerStatesMap.forEach { (bedIndex, vertices) ->
        vertices.forEach { (vertexIndex, state) ->
            LaunchedEffect(state.position) {
                if (bedIndex < viewModel.beds.size && vertexIndex < viewModel.beds[bedIndex].boundary.size) {
                    viewModel.beds[bedIndex].boundary[vertexIndex] = state.position
                }
            }
        }
    }

    LazyColumn(modifier = modifier) {
        // Map
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    // Garden boundary polygon
                    if (viewModel.gardenBoundary.size >= 3) {
                        Polygon(
                            points = viewModel.gardenBoundary.toList(),
                            fillColor = Color(0x3300AA00),
                            strokeColor = Color(0xFF00AA00),
                            strokeWidth = 3f
                        )
                    }

                    // Garden boundary vertex markers
                    gardenMarkerStates.forEach { (_, state) ->
                        Marker(
                            state = state,
                            draggable = true,
                            title = "Garden boundary",
                            alpha = 0.8f
                        )
                    }

                    // Bed polygons and markers
                    viewModel.beds.forEachIndexed { bedIndex, bed ->
                        if (bed.boundary.size >= 3) {
                            Polygon(
                                points = bed.boundary.toList(),
                                fillColor = bed.color.copy(alpha = 0.3f),
                                strokeColor = bed.color,
                                strokeWidth = 2f
                            )
                        }

                        bedMarkerStatesMap[bedIndex]?.forEach { (_, state) ->
                            Marker(
                                state = state,
                                draggable = true,
                                title = bed.name.value,
                                alpha = 0.7f
                            )
                        }
                    }
                }
            }
        }

        // Garden name
        item {
            OutlinedTextField(
                value = viewModel.gardenName,
                onValueChange = { viewModel.gardenName = it },
                label = { Text("Garden Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Garden emoji
        item {
            OutlinedTextField(
                value = viewModel.gardenEmoji,
                onValueChange = { viewModel.gardenEmoji = it },
                label = { Text("Emoji") },
                modifier = Modifier
                    .width(100.dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Section header
        item {
            Text(
                text = "Beds",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Bed cards
        itemsIndexed(viewModel.beds) { index, bed ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = bed.color.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .padding(end = 4.dp)
                        )
                        OutlinedTextField(
                            value = bed.name.value,
                            onValueChange = { bed.name.value = it },
                            label = { Text("Bed Name") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        IconButton(onClick = { viewModel.removeBed(index) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove bed",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bed.description.value,
                        onValueChange = { bed.description.value = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        minLines = 2
                    )
                }
            }
        }

        // Add bed button
        item {
            TextButton(
                onClick = { viewModel.addBed() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add bed")
            }
        }

        // Error
        viewModel.error?.let { errorMsg ->
            item {
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // Create button
        item {
            Button(
                onClick = { viewModel.createGarden() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = viewModel.gardenName.isNotBlank() && !viewModel.isCreating
            ) {
                if (viewModel.isCreating) {
                    CircularProgressIndicator(
                        Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create Garden")
                }
            }
        }
    }
}

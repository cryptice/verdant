@file:Suppress("DEPRECATION")

package app.verdant.android.ui.garden

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.Geocoder
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.*
import app.verdant.android.data.repository.GardenRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import com.google.gson.Gson
import com.google.maps.android.compose.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.cos
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

@Suppress("MissingPermission")
fun getLastKnownLocation(context: Context): GmsLatLng? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    return location?.let { GmsLatLng(it.latitude, it.longitude) }
}

fun List<GmsLatLng>.toJsonString(): String {
    val list = map { mapOf("lat" to it.latitude, "lng" to it.longitude) }
    return Gson().toJson(list)
}

fun midpoint(a: GmsLatLng, b: GmsLatLng) = GmsLatLng(
    (a.latitude + b.latitude) / 2.0,
    (a.longitude + b.longitude) / 2.0
)

private var plusIconCache: BitmapDescriptor? = null

fun getPlusIcon(): BitmapDescriptor {
    plusIconCache?.let { return it }
    val size = 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val bgPaint = Paint().apply { color = 0xFFFFFFFF.toInt(); isAntiAlias = true }
    val fgPaint = Paint().apply { color = 0xFF4CAF50.toInt(); strokeWidth = 5f; isAntiAlias = true }
    val borderPaint = Paint().apply { color = 0xFF388E3C.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = true }
    val r = size / 2f
    canvas.drawCircle(r, r, r - 2, bgPaint)
    canvas.drawCircle(r, r, r - 2, borderPaint)
    canvas.drawLine(r, r - 10, r, r + 10, fgPaint)
    canvas.drawLine(r - 10, r, r + 10, r, fgPaint)
    val desc = BitmapDescriptorFactory.fromBitmap(bitmap)
    plusIconCache = desc
    return desc
}

@HiltViewModel
class CreateGardenViewModel @Inject constructor(
    private val gardenRepository: GardenRepository
) : ViewModel() {

    // Steps: 0=location, 1=boundary, 2=name, 3=beds
    var currentStep by mutableIntStateOf(0)

    // Step 0: Location
    var selectedLatLng by mutableStateOf<GmsLatLng?>(null)
    var selectedAddress by mutableStateOf("")
    var searchQuery by mutableStateOf("")
    var lastCameraZoom by mutableFloatStateOf(20f)

    // Step 1+2: Layout
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
            } catch (_: Exception) {}
        }
    }

    fun generateDefaultLayout(defaultGardenName: String = "My Garden") {
        val latLng = selectedLatLng ?: return
        val lat = latLng.latitude
        val lng = latLng.longitude
        val metersPerDegreeLat = 111_000.0
        val metersPerDegreeLng = 111_000.0 * cos(Math.toRadians(lat))

        val dLat = 20.0 / metersPerDegreeLat / 2.0
        val dLng = 15.0 / metersPerDegreeLng / 2.0

        gardenName = defaultGardenName
        gardenBoundary.clear()
        gardenBoundary.addAll(listOf(
            GmsLatLng(lat - dLat, lng - dLng),
            GmsLatLng(lat - dLat, lng + dLng),
            GmsLatLng(lat + dLat, lng + dLng),
            GmsLatLng(lat + dLat, lng - dLng),
        ))
        beds.clear()
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
                android.util.Log.d("CreateGarden", "Sending request: name=${request.name}, beds=${request.beds.size}")
                val result = gardenRepository.createGardenWithLayout(request)
                android.util.Log.d("CreateGarden", "Created garden id=${result.garden.id}, name=${result.garden.name}")
                createdGardenId = result.garden.id
            } catch (e: Exception) {
                android.util.Log.e("CreateGarden", "Failed to create garden", e)
                error = e.message ?: "Failed to create garden"
            } finally {
                isCreating = false
            }
        }
    }

    fun addBed(defaultBedName: String = "New Bed") {
        beds.add(
            EditableBed(
                name = mutableStateOf(defaultBedName),
                description = mutableStateOf(""),
                boundary = mutableStateListOf(),
                color = bedColors[beds.size % bedColors.size]
            )
        )
    }

    fun removeBed(index: Int) {
        if (index in beds.indices) beds.removeAt(index)
    }

    fun addVertexToBoundary(afterIndex: Int, position: GmsLatLng) {
        gardenBoundary.add(afterIndex + 1, position)
    }

    fun addVertexToBed(bedIndex: Int, afterIndex: Int, position: GmsLatLng) {
        if (bedIndex in beds.indices) {
            beds[bedIndex].boundary.add(afterIndex + 1, position)
        }
    }
}

// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGardenScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: CreateGardenViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(viewModel.createdGardenId) {
        viewModel.createdGardenId?.let { onCreated() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (viewModel.currentStep) {
                            0 -> stringResource(R.string.pick_location)
                            1 -> stringResource(R.string.garden_boundary)
                            2 -> stringResource(R.string.name_your_garden)
                            3 -> stringResource(R.string.garden_beds)
                            else -> "New Garden"
                        }
                    )
                },
                windowInsets = WindowInsets(0),
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.currentStep > 0) viewModel.currentStep--
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        when (viewModel.currentStep) {
            0 -> LocationPickerStep(
                modifier = Modifier.fillMaxSize().padding(padding),
                viewModel = viewModel,
                context = context
            )
            1 -> BoundaryEditorStep(
                modifier = Modifier.fillMaxSize().padding(padding),
                viewModel = viewModel
            )
            2 -> NameGardenStep(
                modifier = Modifier.fillMaxSize().padding(padding),
                viewModel = viewModel
            )
            3 -> BedEditorStep(
                modifier = Modifier.fillMaxSize().padding(padding),
                viewModel = viewModel
            )
        }
    }
}

// ──────────────────────────────────────────────
// Step 0: Location Picker
// ──────────────────────────────────────────────

@Composable
private fun LocationPickerStep(
    modifier: Modifier,
    viewModel: CreateGardenViewModel,
    context: Context
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(getCountryLatLng(), 5f)
    }

    LaunchedEffect(viewModel.selectedLatLng) {
        viewModel.selectedLatLng?.let { latLng ->
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 20f), 1000)
        }
    }

    val hasLocationPermission = remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocationPermission.value = granted
        if (granted) {
            getLastKnownLocation(context)?.let { latLng ->
                viewModel.selectedLatLng = latLng
                viewModel.selectedAddress = ""
                viewModel.searchAddress(context, "${latLng.latitude},${latLng.longitude}") { _, addr ->
                    viewModel.selectedAddress = addr
                    viewModel.searchQuery = addr
                }
            }
        }
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = MapType.HYBRID, maxZoomPreference = 21f),
            onMapClick = { latLng ->
                viewModel.selectedLatLng = latLng
                viewModel.selectedAddress = ""
                viewModel.searchAddress(context, "${latLng.latitude},${latLng.longitude}") { _, addr ->
                    viewModel.selectedAddress = addr
                    viewModel.searchQuery = addr
                }
            }
        ) {
            viewModel.selectedLatLng?.let { latLng ->
                Marker(state = rememberMarkerState(position = latLng), title = "Selected Location")
            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (viewModel.selectedLatLng == null) {
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.searchQuery = it },
                    label = { Text(stringResource(R.string.search_address)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (viewModel.searchQuery.isNotBlank()) {
                                viewModel.searchAddress(context, viewModel.searchQuery) { latLng, addr ->
                                    viewModel.selectedLatLng = latLng
                                    viewModel.selectedAddress = addr
                                    viewModel.searchQuery = addr
                                }
                            }
                        }) { Icon(Icons.Default.Search, stringResource(R.string.search)) }
                    },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
            }

            SmallFloatingActionButton(
                onClick = {
                    if (hasLocationPermission.value) {
                        getLastKnownLocation(context)?.let { latLng ->
                            viewModel.selectedLatLng = latLng
                            viewModel.selectedAddress = ""
                            viewModel.searchAddress(context, "${latLng.latitude},${latLng.longitude}") { _, addr ->
                                viewModel.selectedAddress = addr
                                viewModel.searchQuery = addr
                            }
                        }
                    } else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) { Icon(Icons.Default.MyLocation, stringResource(R.string.my_location), tint = MaterialTheme.colorScheme.primary) }

            if (viewModel.selectedLatLng != null) {
                val defaultGardenName = stringResource(R.string.default_garden_name)
                Spacer(Modifier.height(12.dp))
                SmallFloatingActionButton(
                    onClick = {
                        viewModel.lastCameraZoom = cameraPositionState.position.zoom
                        viewModel.generateDefaultLayout(defaultGardenName)
                        viewModel.currentStep = 1
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) { Icon(Icons.Default.ArrowForward, "Next", tint = MaterialTheme.colorScheme.onPrimary) }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Step 1: Garden Boundary Editor
// ──────────────────────────────────────────────

@Composable
private fun BoundaryEditorStep(
    modifier: Modifier,
    viewModel: CreateGardenViewModel
) {
    val initialPosition = viewModel.selectedLatLng ?: getCountryLatLng()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, viewModel.lastCameraZoom)
    }

    var boundaryVersion by remember { mutableIntStateOf(0) }
    val boundarySize = viewModel.gardenBoundary.size

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = MapType.HYBRID, maxZoomPreference = 21f),
            uiSettings = MapUiSettings(scrollGesturesEnabled = false, zoomGesturesEnabled = false, rotationGesturesEnabled = false, tiltGesturesEnabled = false)
        ) {
            // Garden boundary polygon
            if (boundarySize >= 3) {
                Polygon(
                    points = viewModel.gardenBoundary.toList(),
                    fillColor = Color(0x3300AA00),
                    strokeColor = Color(0xFF00AA00),
                    strokeWidth = 3f
                )
            }

            // Vertex markers (draggable) — each with a stable key
            for (i in 0 until boundarySize) {
                key(boundaryVersion, i) {
                    val state = rememberMarkerState(position = viewModel.gardenBoundary[i])
                    // Sync drag back to viewModel when drag ends
                    LaunchedEffect(state) {
                        snapshotFlow { state.isDragging }.collect { dragging ->
                            if (!dragging && i < viewModel.gardenBoundary.size) {
                                viewModel.gardenBoundary[i] = state.position
                            }
                        }
                    }
                    Marker(state = state, draggable = true, title = stringResource(R.string.drag_to_move), alpha = 0.9f)
                }
            }

            // Midpoint "+" markers on each edge
            if (boundarySize >= 2) {
                val pts = viewModel.gardenBoundary.toList()
                for (i in pts.indices) {
                    val next = (i + 1) % pts.size
                    val mid = midpoint(pts[i], pts[next])
                    val insertAfter = i
                    Marker(
                        state = MarkerState(position = mid),
                        icon = getPlusIcon(),
                        anchor = Offset(0.5f, 0.5f),
                        alpha = 0.85f,
                        onClick = {
                            viewModel.addVertexToBoundary(insertAfter, mid)
                            boundaryVersion++
                            true
                        }
                    )
                }
            }
        }

        // Next FAB
        SmallFloatingActionButton(
            onClick = { viewModel.currentStep = 2 },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) { Icon(Icons.Default.ArrowForward, "Next", tint = MaterialTheme.colorScheme.onPrimary) }

        viewModel.error?.let { msg ->
            Box(Modifier.align(Alignment.TopCenter).padding(16.dp)) {
                app.verdant.android.ui.common.InlineErrorBanner(msg)
            }
        }
    }
}

// ──────────────────────────────────────────────
// Step 2: Name Your Garden
// ──────────────────────────────────────────────

@Composable
private fun NameGardenStep(
    modifier: Modifier,
    viewModel: CreateGardenViewModel
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.what_should_we_call_garden),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = viewModel.gardenEmoji,
            onValueChange = { viewModel.gardenEmoji = it },
            label = { Text(stringResource(R.string.emoji)) },
            modifier = Modifier.width(96.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = viewModel.gardenName,
            onValueChange = { viewModel.gardenName = it },
            label = { Text(stringResource(R.string.garden_name)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { viewModel.currentStep = 3 },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = viewModel.gardenName.isNotBlank()
        ) {
            Text(stringResource(R.string.next_edit_beds))
        }
    }
}

// ──────────────────────────────────────────────
// Step 3: Bed Editor
// ──────────────────────────────────────────────

@Composable
private fun BedEditorStep(
    modifier: Modifier,
    viewModel: CreateGardenViewModel
) {
    val cameraPositionState = rememberCameraPositionState()

    var bedVersion by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        if (viewModel.gardenBoundary.size >= 2) {
            val boundsBuilder = LatLngBounds.builder()
            viewModel.gardenBoundary.forEach { boundsBuilder.include(it) }
            viewModel.beds.forEach { bed -> bed.boundary.forEach { boundsBuilder.include(it) } }
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100), 1000)
        }
    }

    LazyColumn(modifier = modifier) {
        // Map
        item {
            Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(mapType = MapType.HYBRID, maxZoomPreference = 21f),
                    uiSettings = MapUiSettings(scrollGesturesEnabled = false, zoomGesturesEnabled = false, rotationGesturesEnabled = false, tiltGesturesEnabled = false)
                ) {
                    // Garden boundary as reference (non-editable)
                    if (viewModel.gardenBoundary.size >= 3) {
                        Polygon(
                            points = viewModel.gardenBoundary.toList(),
                            fillColor = Color(0x1100AA00),
                            strokeColor = Color(0xFF00AA00),
                            strokeWidth = 2f
                        )
                    }

                    // Bed polygons, vertex markers, and midpoint "+" markers
                    viewModel.beds.forEachIndexed { bedIndex, bed ->
                        if (bed.boundary.size >= 3) {
                            Polygon(
                                points = bed.boundary.toList(),
                                fillColor = bed.color.copy(alpha = 0.3f),
                                strokeColor = bed.color,
                                strokeWidth = 2f
                            )
                        }

                        // Vertex markers
                        for (vi in 0 until bed.boundary.size) {
                            key(bedVersion, bedIndex, vi) {
                                val state = rememberMarkerState(position = bed.boundary[vi])
                                LaunchedEffect(state) {
                                    snapshotFlow { state.isDragging }.collect { dragging ->
                                        if (!dragging && bedIndex < viewModel.beds.size && vi < viewModel.beds[bedIndex].boundary.size) {
                                            viewModel.beds[bedIndex].boundary[vi] = state.position
                                        }
                                    }
                                }
                                Marker(state = state, draggable = true, title = bed.name.value, alpha = 0.8f)
                            }
                        }

                        // Midpoint "+" markers
                        if (bed.boundary.size >= 2) {
                            val pts = bed.boundary.toList()
                            for (i in pts.indices) {
                                val next = (i + 1) % pts.size
                                val mid = midpoint(pts[i], pts[next])
                                val insertAfter = i
                                val bi = bedIndex
                                Marker(
                                    state = MarkerState(position = mid),
                                    icon = getPlusIcon(),
                                    anchor = Offset(0.5f, 0.5f),
                                    alpha = 0.85f,
                                    onClick = {
                                        viewModel.addVertexToBed(bi, insertAfter, mid)
                                        bedVersion++
                                        true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section header
        item {
            Text(
                stringResource(R.string.beds), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Bed cards
        itemsIndexed(viewModel.beds) { index, bed ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = bed.color.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = bed.name.value,
                            onValueChange = { bed.name.value = it },
                            label = { Text(stringResource(R.string.bed_name)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        IconButton(onClick = { viewModel.removeBed(index) }) {
                            Icon(Icons.Default.Delete, "Remove bed", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bed.description.value,
                        onValueChange = { bed.description.value = it },
                        label = { Text(stringResource(R.string.description)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        minLines = 2
                    )
                }
            }
        }

        // Add bed button
        item {
            val defaultBedName = stringResource(R.string.default_bed_name)
            TextButton(
                onClick = { viewModel.addBed(defaultBedName) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.add_bed))
            }
        }

        // Error
        viewModel.error?.let { errorMsg ->
            item {
                app.verdant.android.ui.common.InlineErrorBanner(errorMsg, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }

        // Create button
        item {
            Button(
                onClick = { viewModel.createGarden() },
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = viewModel.gardenName.isNotBlank() && !viewModel.isCreating
            ) {
                if (viewModel.isCreating) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.create_garden))
                }
            }
        }
    }
}

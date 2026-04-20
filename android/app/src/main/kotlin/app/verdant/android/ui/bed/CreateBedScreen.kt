package app.verdant.android.ui.bed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.BedAspect
import app.verdant.android.data.model.BedDrainage
import app.verdant.android.data.model.BedIrrigationType
import app.verdant.android.data.model.BedProtection
import app.verdant.android.data.model.BedSoilType
import app.verdant.android.data.model.BedSunExposure
import app.verdant.android.data.model.CreateBedRequest
import app.verdant.android.ui.theme.verdantTopAppBarColors
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateBedState(
    val isLoading: Boolean = false,
    val createdId: Long? = null,
    val error: String? = null
)

@HiltViewModel
class CreateBedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gardenRepository: GardenRepository
) : ViewModel() {
    private val gardenId: Long = savedStateHandle.get<Long>("gardenId")!!
    private val _uiState = MutableStateFlow(CreateBedState())
    val uiState = _uiState.asStateFlow()

    fun create(
        name: String,
        description: String,
        soilType: String?,
        soilPh: Double?,
        sunExposure: String?,
        drainage: String?,
        aspect: String?,
        irrigationType: String?,
        protection: String?,
        raisedBed: Boolean?
    ) {
        viewModelScope.launch {
            _uiState.value = CreateBedState(isLoading = true)
            try {
                val bed = gardenRepository.createBed(
                    gardenId,
                    CreateBedRequest(
                        name = name,
                        description = description.ifBlank { null },
                        soilType = soilType,
                        soilPh = soilPh,
                        sunExposure = sunExposure,
                        drainage = drainage,
                        aspect = aspect,
                        irrigationType = irrigationType,
                        protection = protection,
                        raisedBed = raisedBed
                    )
                )
                _uiState.value = CreateBedState(createdId = bed.id)
            } catch (e: Exception) {
                _uiState.value = CreateBedState(error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBedScreen(
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
    viewModel: CreateBedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var conditionsExpanded by remember { mutableStateOf(false) }

    // Conditions state
    var soilType by remember { mutableStateOf<String?>(null) }
    var soilPhText by remember { mutableStateOf("") }
    var sunExposure by remember { mutableStateOf<String?>(null) }
    var aspect by remember { mutableStateOf<String?>(null) }
    var drainage by remember { mutableStateOf<String?>(null) }
    var irrigationType by remember { mutableStateOf<String?>(null) }
    var protection by remember { mutableStateOf<String?>(null) }
    var raisedBed by remember { mutableStateOf<Boolean?>(null) }

    val soilPhValue = soilPhText.toDoubleOrNull()
    val soilPhError = soilPhText.isNotBlank() && (soilPhValue == null || soilPhValue < 3.0 || soilPhValue > 9.0)

    LaunchedEffect(uiState.createdId) {
        uiState.createdId?.let { onCreated(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_bed)) },
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.bed_name)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description_optional)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            // Conditions section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.bed_conditions_section_title),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { conditionsExpanded = !conditionsExpanded }) {
                            Icon(
                                if (conditionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }
                    AnimatedVisibility(visible = conditionsExpanded) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            BedConditionsFields(
                                soilType = soilType,
                                onSoilTypeChange = { soilType = it },
                                soilPhText = soilPhText,
                                onSoilPhTextChange = { soilPhText = it },
                                soilPhError = soilPhError,
                                sunExposure = sunExposure,
                                onSunExposureChange = { sunExposure = it },
                                aspect = aspect,
                                onAspectChange = { aspect = it },
                                drainage = drainage,
                                onDrainageChange = { drainage = it },
                                irrigationType = irrigationType,
                                onIrrigationTypeChange = { irrigationType = it },
                                protection = protection,
                                onProtectionChange = { protection = it },
                                raisedBed = raisedBed,
                                onRaisedBedChange = { raisedBed = it }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.create(
                        name = name,
                        description = description,
                        soilType = soilType,
                        soilPh = soilPhText.toDoubleOrNull(),
                        sunExposure = sunExposure,
                        drainage = drainage,
                        aspect = aspect,
                        irrigationType = irrigationType,
                        protection = protection,
                        raisedBed = raisedBed
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = name.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.create_bed))
                }
            }
            uiState.error?.let { app.verdant.android.ui.common.InlineErrorBanner(it) }
        }
    }
}

@Composable
fun bedSoilTypeLabel(value: String): String = when (value) {
    BedSoilType.SANDY -> stringResource(R.string.bed_soil_sandy)
    BedSoilType.LOAMY -> stringResource(R.string.bed_soil_loamy)
    BedSoilType.CLAY -> stringResource(R.string.bed_soil_clay)
    BedSoilType.SILTY -> stringResource(R.string.bed_soil_silty)
    BedSoilType.PEATY -> stringResource(R.string.bed_soil_peaty)
    BedSoilType.CHALKY -> stringResource(R.string.bed_soil_chalky)
    else -> value
}

@Composable
fun bedSunExposureLabel(value: String): String = when (value) {
    BedSunExposure.FULL_SUN -> stringResource(R.string.bed_sun_full_sun)
    BedSunExposure.PARTIAL_SUN -> stringResource(R.string.bed_sun_partial_sun)
    BedSunExposure.PARTIAL_SHADE -> stringResource(R.string.bed_sun_partial_shade)
    BedSunExposure.FULL_SHADE -> stringResource(R.string.bed_sun_full_shade)
    else -> value
}

@Composable
fun bedDrainageLabel(value: String): String = when (value) {
    BedDrainage.POOR -> stringResource(R.string.bed_drainage_poor)
    BedDrainage.MODERATE -> stringResource(R.string.bed_drainage_moderate)
    BedDrainage.GOOD -> stringResource(R.string.bed_drainage_good)
    BedDrainage.SHARP -> stringResource(R.string.bed_drainage_sharp)
    else -> value
}

@Composable
fun bedAspectLabel(value: String): String = when (value) {
    BedAspect.FLAT -> stringResource(R.string.bed_aspect_flat)
    BedAspect.N -> stringResource(R.string.bed_aspect_n)
    BedAspect.NE -> stringResource(R.string.bed_aspect_ne)
    BedAspect.E -> stringResource(R.string.bed_aspect_e)
    BedAspect.SE -> stringResource(R.string.bed_aspect_se)
    BedAspect.S -> stringResource(R.string.bed_aspect_s)
    BedAspect.SW -> stringResource(R.string.bed_aspect_sw)
    BedAspect.W -> stringResource(R.string.bed_aspect_w)
    BedAspect.NW -> stringResource(R.string.bed_aspect_nw)
    else -> value
}

@Composable
fun bedIrrigationTypeLabel(value: String): String = when (value) {
    BedIrrigationType.DRIP -> stringResource(R.string.bed_irrigation_drip)
    BedIrrigationType.SPRINKLER -> stringResource(R.string.bed_irrigation_sprinkler)
    BedIrrigationType.SOAKER_HOSE -> stringResource(R.string.bed_irrigation_soaker_hose)
    BedIrrigationType.MANUAL -> stringResource(R.string.bed_irrigation_manual)
    BedIrrigationType.NONE -> stringResource(R.string.bed_irrigation_none)
    else -> value
}

@Composable
fun bedProtectionLabel(value: String): String = when (value) {
    BedProtection.OPEN_FIELD -> stringResource(R.string.bed_protection_open_field)
    BedProtection.ROW_COVER -> stringResource(R.string.bed_protection_row_cover)
    BedProtection.LOW_TUNNEL -> stringResource(R.string.bed_protection_low_tunnel)
    BedProtection.HIGH_TUNNEL -> stringResource(R.string.bed_protection_high_tunnel)
    BedProtection.GREENHOUSE -> stringResource(R.string.bed_protection_greenhouse)
    BedProtection.COLDFRAME -> stringResource(R.string.bed_protection_coldframe)
    else -> value
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun BedConditionsFields(
    soilType: String?,
    onSoilTypeChange: (String?) -> Unit,
    soilPhText: String,
    onSoilPhTextChange: (String) -> Unit,
    soilPhError: Boolean,
    sunExposure: String?,
    onSunExposureChange: (String?) -> Unit,
    aspect: String?,
    onAspectChange: (String?) -> Unit,
    drainage: String?,
    onDrainageChange: (String?) -> Unit,
    irrigationType: String?,
    onIrrigationTypeChange: (String?) -> Unit,
    protection: String?,
    onProtectionChange: (String?) -> Unit,
    raisedBed: Boolean?,
    onRaisedBedChange: (Boolean?) -> Unit,
) {
    // Soil type
    Text(stringResource(R.string.bed_soil_type), fontWeight = FontWeight.Medium, fontSize = 13.sp)
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BedSoilType.values.forEach { v ->
            FilterChip(
                selected = soilType == v,
                onClick = { onSoilTypeChange(if (soilType == v) null else v) },
                label = { Text(bedSoilTypeLabel(v), fontSize = 12.sp) }
            )
        }
    }

    // Soil pH
    OutlinedTextField(
        value = soilPhText,
        onValueChange = onSoilPhTextChange,
        label = { Text(stringResource(R.string.bed_soil_ph)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = soilPhError,
        supportingText = if (soilPhError) {
            { Text(stringResource(R.string.bed_soil_ph_error)) }
        } else null,
        singleLine = true
    )

    // Sun exposure
    Text(stringResource(R.string.bed_sun_exposure), fontWeight = FontWeight.Medium, fontSize = 13.sp)
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BedSunExposure.values.forEach { v ->
            FilterChip(
                selected = sunExposure == v,
                onClick = { onSunExposureChange(if (sunExposure == v) null else v) },
                label = { Text(bedSunExposureLabel(v), fontSize = 12.sp) }
            )
        }
    }

    // Aspect
    Text(stringResource(R.string.bed_aspect), fontWeight = FontWeight.Medium, fontSize = 13.sp)
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BedAspect.values.forEach { v ->
            FilterChip(
                selected = aspect == v,
                onClick = { onAspectChange(if (aspect == v) null else v) },
                label = { Text(bedAspectLabel(v), fontSize = 12.sp) }
            )
        }
    }

    // Drainage
    Text(stringResource(R.string.bed_drainage), fontWeight = FontWeight.Medium, fontSize = 13.sp)
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BedDrainage.values.forEach { v ->
            FilterChip(
                selected = drainage == v,
                onClick = { onDrainageChange(if (drainage == v) null else v) },
                label = { Text(bedDrainageLabel(v), fontSize = 12.sp) }
            )
        }
    }

    // Irrigation type
    Text(stringResource(R.string.bed_irrigation), fontWeight = FontWeight.Medium, fontSize = 13.sp)
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BedIrrigationType.values.forEach { v ->
            FilterChip(
                selected = irrigationType == v,
                onClick = { onIrrigationTypeChange(if (irrigationType == v) null else v) },
                label = { Text(bedIrrigationTypeLabel(v), fontSize = 12.sp) }
            )
        }
    }

    // Protection
    Text(stringResource(R.string.bed_protection), fontWeight = FontWeight.Medium, fontSize = 13.sp)
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BedProtection.values.forEach { v ->
            FilterChip(
                selected = protection == v,
                onClick = { onProtectionChange(if (protection == v) null else v) },
                label = { Text(bedProtectionLabel(v), fontSize = 12.sp) }
            )
        }
    }

    // Raised bed
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(R.string.bed_raised_bed), fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Switch(
            checked = raisedBed == true,
            onCheckedChange = { checked -> onRaisedBedChange(if (checked) true else null) }
        )
    }
}

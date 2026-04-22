package app.verdant.android.ui.bed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
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
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.faltet.FaltetChipSelector
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.Field
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
    var soilType by remember { mutableStateOf<String?>(null) }
    var soilPhText by remember { mutableStateOf("") }
    var drainage by remember { mutableStateOf<String?>(null) }
    var sunExposure by remember { mutableStateOf<String?>(null) }
    var aspect by remember { mutableStateOf<String?>(null) }
    var irrigationType by remember { mutableStateOf<String?>(null) }
    var protection by remember { mutableStateOf<String?>(null) }
    var raisedBed by remember { mutableStateOf<Boolean?>(null) }
    var nameError by remember { mutableStateOf(false) }
    var phError by remember { mutableStateOf<String?>(null) }

    val canSubmit = name.isNotBlank() && phError == null && !uiState.isLoading

    val submitAction: () -> Unit = {
        val phValue = soilPhText.toDoubleOrNull()
        val phInvalid = soilPhText.isNotBlank() && (phValue == null || phValue < 3.0 || phValue > 9.0)
        phError = if (phInvalid) "pH måste vara mellan 3.0 och 9.0" else null
        nameError = name.isBlank()
        if (!nameError && phError == null) {
            viewModel.create(
                name = name,
                description = description,
                soilType = soilType,
                soilPh = phValue,
                sunExposure = sunExposure,
                aspect = aspect,
                drainage = drainage,
                irrigationType = irrigationType,
                protection = protection,
                raisedBed = raisedBed,
            )
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }
    LaunchedEffect(uiState.createdId) {
        if (uiState.createdId != null) onCreated(uiState.createdId!!)
    }

    FaltetScreenScaffold(
        mastheadLeft = "§ Bädd",
        mastheadCenter = "Ny bädd",
        bottomBar = {
            FaltetFormSubmitBar(
                label = "Skapa",
                onClick = submitAction,
                enabled = canSubmit,
                submitting = uiState.isLoading,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Field(
                    label = "Namn",
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    required = true,
                    error = if (nameError) "Namn krävs" else null,
                )
            }
            item {
                Field(
                    label = "Beskrivning (valfri)",
                    value = description,
                    onValueChange = { description = it },
                )
            }
            item {
                FaltetChipSelector(
                    label = "Jordtyp",
                    options = BedSoilType.values,
                    selected = soilType,
                    onSelectedChange = { soilType = it },
                    labelFor = { bedSoilTypeLabelStr(it) },
                )
            }
            item {
                Field(
                    label = "pH (valfri)",
                    value = soilPhText,
                    onValueChange = { soilPhText = it; phError = null },
                    keyboardType = KeyboardType.Decimal,
                    error = phError,
                )
            }
            item {
                FaltetChipSelector(
                    label = "Dränering",
                    options = BedDrainage.values,
                    selected = drainage,
                    onSelectedChange = { drainage = it },
                    labelFor = { bedDrainageLabelStr(it) },
                )
            }
            item {
                FaltetChipSelector(
                    label = "Sol",
                    options = BedSunExposure.values,
                    selected = sunExposure,
                    onSelectedChange = { sunExposure = it },
                    labelFor = { bedSunExposureLabelStr(it) },
                )
            }
            item {
                FaltetChipSelector(
                    label = "Väderstreck",
                    options = BedAspect.values,
                    selected = aspect,
                    onSelectedChange = { aspect = it },
                    labelFor = { bedAspectLabelStr(it) },
                )
            }
            item {
                FaltetChipSelector(
                    label = "Bevattning",
                    options = BedIrrigationType.values,
                    selected = irrigationType,
                    onSelectedChange = { irrigationType = it },
                    labelFor = { bedIrrigationTypeLabelStr(it) },
                )
            }
            item {
                FaltetChipSelector(
                    label = "Skydd",
                    options = BedProtection.values,
                    selected = protection,
                    onSelectedChange = { protection = it },
                    labelFor = { bedProtectionLabelStr(it) },
                )
            }
            item {
                FaltetChipSelector(
                    label = "Upphöjd bädd (valfri)",
                    options = listOf(true, false),
                    selected = raisedBed,
                    onSelectedChange = { raisedBed = it },
                    labelFor = { if (it) "Ja" else "Nej" },
                )
            }
        }
    }
}

// BedConditionsFields — kept for use by BedDetailScreen (edit dialog)
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
    FaltetChipSelector(
        label = "Jordtyp",
        options = BedSoilType.values,
        selected = soilType,
        onSelectedChange = onSoilTypeChange,
        labelFor = { bedSoilTypeLabelStr(it) },
    )
    FaltetChipSelector(
        label = "Sol",
        options = BedSunExposure.values,
        selected = sunExposure,
        onSelectedChange = onSunExposureChange,
        labelFor = { bedSunExposureLabelStr(it) },
    )
    FaltetChipSelector(
        label = "Väderstreck",
        options = BedAspect.values,
        selected = aspect,
        onSelectedChange = onAspectChange,
        labelFor = { bedAspectLabelStr(it) },
    )
    FaltetChipSelector(
        label = "Dränering",
        options = BedDrainage.values,
        selected = drainage,
        onSelectedChange = onDrainageChange,
        labelFor = { bedDrainageLabelStr(it) },
    )
    FaltetChipSelector(
        label = "Bevattning",
        options = BedIrrigationType.values,
        selected = irrigationType,
        onSelectedChange = onIrrigationTypeChange,
        labelFor = { bedIrrigationTypeLabelStr(it) },
    )
    FaltetChipSelector(
        label = "Skydd",
        options = BedProtection.values,
        selected = protection,
        onSelectedChange = onProtectionChange,
        labelFor = { bedProtectionLabelStr(it) },
    )
    Field(
        label = "pH (valfri)",
        value = soilPhText,
        onValueChange = onSoilPhTextChange,
        keyboardType = KeyboardType.Decimal,
        error = if (soilPhError) "pH måste vara mellan 3.0 och 9.0" else null,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Upphöjd bädd", fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Switch(
            checked = raisedBed == true,
            onCheckedChange = { checked -> onRaisedBedChange(if (checked) true else null) }
        )
    }
}

// Non-composable label functions — used by FaltetChipSelector.labelFor (plain (T) -> String lambda)
fun bedSoilTypeLabelStr(value: String): String = when (value) {
    BedSoilType.SANDY -> "Sand"
    BedSoilType.LOAMY -> "Mulljord"
    BedSoilType.CLAY -> "Lera"
    BedSoilType.SILTY -> "Siltjord"
    BedSoilType.PEATY -> "Torv"
    BedSoilType.CHALKY -> "Kalkjord"
    else -> value
}

fun bedSunExposureLabelStr(value: String): String = when (value) {
    BedSunExposure.FULL_SUN -> "Fullt sol"
    BedSunExposure.PARTIAL_SUN -> "Halvskugga/sol"
    BedSunExposure.PARTIAL_SHADE -> "Halvskugga"
    BedSunExposure.FULL_SHADE -> "Skugga"
    else -> value
}

fun bedDrainageLabelStr(value: String): String = when (value) {
    BedDrainage.POOR -> "Dålig"
    BedDrainage.MODERATE -> "Måttlig"
    BedDrainage.GOOD -> "Bra"
    BedDrainage.SHARP -> "Skarp"
    else -> value
}

fun bedAspectLabelStr(value: String): String = when (value) {
    BedAspect.FLAT -> "Plant"
    BedAspect.N -> "N"
    BedAspect.NE -> "NO"
    BedAspect.E -> "O"
    BedAspect.SE -> "SO"
    BedAspect.S -> "S"
    BedAspect.SW -> "SV"
    BedAspect.W -> "V"
    BedAspect.NW -> "NV"
    else -> value
}

fun bedIrrigationTypeLabelStr(value: String): String = when (value) {
    BedIrrigationType.DRIP -> "Droppbevattning"
    BedIrrigationType.SPRINKLER -> "Spridare"
    BedIrrigationType.SOAKER_HOSE -> "Soakerslang"
    BedIrrigationType.MANUAL -> "Manuell"
    BedIrrigationType.NONE -> "Ingen"
    else -> value
}

fun bedProtectionLabelStr(value: String): String = when (value) {
    BedProtection.OPEN_FIELD -> "Öppet fält"
    BedProtection.ROW_COVER -> "Radtäcke"
    BedProtection.LOW_TUNNEL -> "Låg tunnel"
    BedProtection.HIGH_TUNNEL -> "Hög tunnel"
    BedProtection.GREENHOUSE -> "Växthus"
    BedProtection.COLDFRAME -> "Kallbänk"
    else -> value
}

// Composable label functions — used by BedDetailScreen and other composable call sites
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

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun CreateBedScreenPreview() {
    // Populated form state with a pH error for visual inspection
    val snackbarHostState = remember { SnackbarHostState() }
    FaltetScreenScaffold(
        mastheadLeft = "§ Bädd",
        mastheadCenter = "Ny bädd",
        bottomBar = {
            FaltetFormSubmitBar(
                label = "Skapa",
                onClick = {},
                enabled = false,
                submitting = false,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Field(
                    label = "Namn",
                    value = "Köksträdgård",
                    onValueChange = {},
                    required = true,
                    error = null,
                )
            }
            item {
                Field(
                    label = "Beskrivning (valfri)",
                    value = "Södra delen av trädgården",
                    onValueChange = {},
                )
            }
            item {
                FaltetChipSelector(
                    label = "Jordtyp",
                    options = BedSoilType.values,
                    selected = BedSoilType.LOAMY,
                    onSelectedChange = {},
                    labelFor = { it },
                )
            }
            item {
                Field(
                    label = "pH (valfri)",
                    value = "2.0",
                    onValueChange = {},
                    keyboardType = KeyboardType.Decimal,
                    error = "pH måste vara mellan 3.0 och 9.0",
                )
            }
            item {
                FaltetChipSelector(
                    label = "Dränering",
                    options = BedDrainage.values,
                    selected = BedDrainage.GOOD,
                    onSelectedChange = {},
                    labelFor = { it },
                )
            }
        }
    }
}

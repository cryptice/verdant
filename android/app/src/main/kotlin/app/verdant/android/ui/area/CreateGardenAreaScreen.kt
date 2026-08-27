package app.verdant.android.ui.area
import app.verdant.android.data.repository.GardenAreaRepository

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.AREA_CATEGORIES
import app.verdant.android.data.model.CreateGardenAreaRequest
import app.verdant.android.ui.faltet.FaltetChipSelector
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.Field
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateGardenAreaState(
    val isLoading: Boolean = false,
    val createdId: Long? = null,
    val error: String? = null
)

@HiltViewModel
class CreateGardenAreaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val areaRepository: GardenAreaRepository
) : ViewModel() {
    private val gardenId: Long = savedStateHandle.get<Long>("gardenId")!!
    private val _uiState = MutableStateFlow(CreateGardenAreaState())
    val uiState = _uiState.asStateFlow()

    fun create(
        name: String,
        category: String,
        description: String,
        sizeSqm: Double?
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _uiState.value = CreateGardenAreaState(isLoading = true)
            try {
                val area = areaRepository.create(
                    gardenId,
                    CreateGardenAreaRequest(
                        name = name,
                        category = category,
                        description = description.ifBlank { null },
                        sizeSqm = sizeSqm
                    )
                )
                _uiState.value = CreateGardenAreaState(createdId = area.id)
            } catch (e: Exception) {
                _uiState.value = CreateGardenAreaState(error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGardenAreaScreen(
    onCreated: (Long) -> Unit,
    viewModel: CreateGardenAreaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var sizeText by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    val categoryLabels = AREA_CATEGORIES.associateWith { stringResource(areaCategoryLabelRes(it)) }

    val canSubmit = name.isNotBlank() && category != null && !uiState.isLoading

    val nameRequiredMessage = stringResource(R.string.area_field_name_required)

    val submitAction: () -> Unit = {
        nameError = name.isBlank()
        val selectedCategory = category
        if (!nameError && selectedCategory != null) {
            viewModel.create(
                name = name,
                category = selectedCategory,
                description = description,
                sizeSqm = sizeText.toDoubleOrNull(),
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
        mastheadLeft = "",
        mastheadCenter = stringResource(R.string.area_create_title),
        bottomBar = {
            FaltetFormSubmitBar(
                label = stringResource(R.string.create),
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
                    label = stringResource(R.string.area_field_name),
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    required = true,
                    error = if (nameError) nameRequiredMessage else null,
                )
            }
            item {
                FaltetChipSelector(
                    label = stringResource(R.string.area_field_category),
                    options = AREA_CATEGORIES,
                    selected = category,
                    onSelectedChange = { category = it },
                    labelFor = { categoryLabels[it] ?: it },
                    required = true,
                )
            }
            item {
                Field(
                    label = stringResource(R.string.area_field_description),
                    value = description,
                    onValueChange = { description = it },
                )
            }
            item {
                Field(
                    label = stringResource(R.string.area_field_size),
                    value = sizeText,
                    onValueChange = { sizeText = it },
                    keyboardType = KeyboardType.Decimal,
                )
            }
        }
    }
}

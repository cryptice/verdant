package app.verdant.android.ui.plant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreatePlantRequest
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatePlantState(
    val isLoading: Boolean = false,
    val created: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CreatePlantViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gardenRepository: GardenRepository
) : ViewModel() {
    private val bedId: Long = savedStateHandle.get<Long>("bedId")!!
    private val _uiState = MutableStateFlow(CreatePlantState())
    val uiState = _uiState.asStateFlow()

    fun create(name: String, species: String) {
        viewModelScope.launch {
            _uiState.value = CreatePlantState(isLoading = true)
            try {
                gardenRepository.createPlant(bedId, CreatePlantRequest(name, species.ifBlank { null }))
                _uiState.value = CreatePlantState(created = true)
            } catch (e: Exception) {
                _uiState.value = CreatePlantState(error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlantScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: CreatePlantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }

    LaunchedEffect(uiState.created) {
        if (uiState.created) onCreated()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Plant") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Plant Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = species,
                onValueChange = { species = it },
                label = { Text("Species (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.create(name, species) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = name.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Add Plant")
                }
            }
            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

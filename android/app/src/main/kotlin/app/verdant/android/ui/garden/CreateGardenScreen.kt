package app.verdant.android.ui.garden

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreateGardenRequest
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateGardenState(
    val isLoading: Boolean = false,
    val createdId: Long? = null,
    val error: String? = null
)

@HiltViewModel
class CreateGardenViewModel @Inject constructor(
    private val gardenRepository: GardenRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateGardenState())
    val uiState = _uiState.asStateFlow()

    fun create(name: String, description: String, emoji: String) {
        viewModelScope.launch {
            _uiState.value = CreateGardenState(isLoading = true)
            try {
                val garden = gardenRepository.createGarden(
                    CreateGardenRequest(name, description.ifBlank { null }, emoji.ifBlank { "\uD83C\uDF31" })
                )
                _uiState.value = CreateGardenState(createdId = garden.id)
            } catch (e: Exception) {
                _uiState.value = CreateGardenState(error = e.message)
            }
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
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("\uD83C\uDF31") }

    LaunchedEffect(uiState.createdId) {
        uiState.createdId?.let { onCreated(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Garden") },
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
                value = emoji,
                onValueChange = { emoji = it },
                label = { Text("Emoji") },
                modifier = Modifier.width(100.dp),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Garden Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.create(name, description, emoji) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = name.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Create Garden")
                }
            }
            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

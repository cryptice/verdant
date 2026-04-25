// android/app/src/main/kotlin/app/verdant/android/ui/bouquet/BouquetsScreen.kt
package app.verdant.android.ui.bouquet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.BouquetRecipeResponse
import app.verdant.android.data.model.BouquetResponse
import app.verdant.android.data.model.CreateBouquetItemRequest
import app.verdant.android.data.model.CreateBouquetRequest
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetChipSelector
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.theme.FaltetForest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BouquetsState(
    val isLoading: Boolean = true,
    val bouquets: List<BouquetResponse> = emptyList(),
    val recipes: List<BouquetRecipeResponse> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class BouquetsViewModel @Inject constructor(
    private val repo: GardenRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BouquetsState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val bouquets = repo.getBouquets()
                val recipes = runCatching { repo.getBouquetRecipes() }.getOrDefault(emptyList())
                _uiState.value = BouquetsState(isLoading = false, bouquets = bouquets, recipes = recipes)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    /** Quick-build: create a bouquet from a recipe (copying items) or as an
     *  empty named bouquet that the user can finish editing on web. */
    fun build(name: String, recipe: BouquetRecipeResponse?) {
        viewModelScope.launch {
            try {
                repo.createBouquet(
                    CreateBouquetRequest(
                        sourceRecipeId = recipe?.id,
                        name = name.ifBlank { recipe?.name ?: "Bukett" },
                        priceSek = recipe?.priceSek,
                        items = recipe?.items?.map {
                            CreateBouquetItemRequest(
                                speciesId = it.speciesId,
                                stemCount = it.stemCount,
                                role = it.role,
                                notes = it.notes,
                            )
                        } ?: emptyList(),
                    )
                )
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            try { repo.deleteBouquet(id); refresh() } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BouquetsScreen(
    onBack: () -> Unit,
    viewModel: BouquetsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBuildDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<BouquetResponse?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    if (showBuildDialog) {
        BuildBouquetDialog(
            recipes = uiState.recipes,
            onDismiss = { showBuildDialog = false },
            onBuild = { name, recipe -> viewModel.build(name, recipe); showBuildDialog = false },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Radera bukett") },
            text = { Text("Radera \"${target.name}\"?") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(target.id); deleteTarget = null }) { Text("Radera") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Avbryt") } },
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = "§ Försäljning",
        mastheadCenter = "Buketter",
        fab = { FaltetFab(onClick = { showBuildDialog = true }, contentDescription = "Bygg bukett") },
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            uiState.bouquets.isEmpty() -> FaltetEmptyState(
                headline = "Inga buketter byggda än",
                subtitle = "Tryck på + för att binda en ny.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(uiState.bouquets, key = { it.id }) { b ->
                    FaltetListRow(
                        title = b.name,
                        meta = listOfNotNull(
                            b.assembledAt.take(10),
                            b.sourceRecipeName?.let { "från $it" },
                            "${b.items.sumOf { it.stemCount }} st",
                            b.priceSek?.let { "$it kr" },
                        ).joinToString(" · "),
                        onClick = { deleteTarget = b },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuildBouquetDialog(
    recipes: List<BouquetRecipeResponse>,
    onDismiss: () -> Unit,
    onBuild: (name: String, recipe: BouquetRecipeResponse?) -> Unit,
) {
    var selectedRecipe by remember { mutableStateOf<BouquetRecipeResponse?>(null) }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bygg bukett") },
        text = {
            Column {
                Text(
                    "Välj ett recept eller lämna tomt för en ny bukett från grunden.",
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    color = FaltetForest,
                )
                Spacer(Modifier.height(12.dp))
                FaltetChipSelector(
                    label = "Recept (valfritt)",
                    options = recipes,
                    selected = selectedRecipe,
                    onSelectedChange = { selectedRecipe = it },
                    labelFor = { it.name },
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Namn (valfritt)") },
                    placeholder = { Text(selectedRecipe?.name ?: "Bukett") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onBuild(name, selectedRecipe) }) { Text("Bygg") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}

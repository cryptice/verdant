// android/app/src/main/kotlin/app/verdant/android/ui/bouquet/BouquetsScreen.kt
package app.verdant.android.ui.bouquet
import app.verdant.android.ui.faltet.BotanicalPlate
import app.verdant.android.data.repository.BouquetRepository
import app.verdant.android.data.repository.SpeciesRepository

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.BouquetRecipeResponse
import app.verdant.android.data.model.BouquetResponse
import app.verdant.android.data.model.CreateBouquetItemRequest
import app.verdant.android.data.model.CreateBouquetRequest
import app.verdant.android.data.model.ItemRole
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.UpdateBouquetRequest
import app.verdant.android.data.model.sortedBySwedishName
import app.verdant.android.ui.common.ConnectionErrorState
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
    val saving: Boolean = false,
    val bouquets: List<BouquetResponse> = emptyList(),
    val recipes: List<BouquetRecipeResponse> = emptyList(),
    val species: List<SpeciesResponse> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class BouquetsViewModel @Inject constructor(
    private val bouquetRepository: BouquetRepository,
    private val speciesRepository: SpeciesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BouquetsState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val bouquets = bouquetRepository.list()
                val recipes = runCatching { bouquetRepository.listRecipes() }.getOrDefault(emptyList())
                val species = runCatching { speciesRepository.list().sortedBySwedishName() }.getOrDefault(emptyList())
                _uiState.value = BouquetsState(
                    isLoading = false,
                    bouquets = bouquets,
                    recipes = recipes,
                    species = species,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun save(
        existing: BouquetResponse?,
        sourceRecipeId: Long?,
        name: String,
        priceCents: Int?,
        items: List<CreateBouquetItemRequest>,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, error = null)
            try {
                if (existing == null) {
                    // Phase 10 will add the outlet picker to this dialog. For
                    // now the create call is wired with placeholder ids; the
                    // backend will reject if outletId 0 doesn't exist.
                    bouquetRepository.create(
                        CreateBouquetRequest(
                            sourceRecipeId = sourceRecipeId,
                            name = name,
                            items = items,
                            outletId = 0L,
                            initialRequestedPriceCents = priceCents ?: 0,
                        )
                    )
                } else {
                    bouquetRepository.update(
                        existing.id,
                        UpdateBouquetRequest(
                            sourceRecipeId = sourceRecipeId,
                            name = name,
                            items = items,
                        )
                    )
                }
                _uiState.value = _uiState.value.copy(saving = false)
                onDone()
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = e.message)
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            try { bouquetRepository.delete(id); refresh() } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BouquetsScreen(
    onBack: () -> Unit,
    onOpenRecipes: () -> Unit = {},
    viewModel: BouquetsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editorTarget by remember { mutableStateOf<EditorTarget?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    editorTarget?.let { target ->
        BouquetEditorDialog(
            existing = target.existing,
            recipes = uiState.recipes,
            species = uiState.species,
            saving = uiState.saving,
            onDismiss = { editorTarget = null },
            onSave = { sourceId, name, price, items ->
                viewModel.save(target.existing, sourceId, name, price, items) {
                    editorTarget = null
                }
            },
            onDelete = target.existing?.let { e -> { viewModel.delete(e.id); editorTarget = null } },
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Buketter",
        mastheadRight = {
            TextButton(onClick = onOpenRecipes) { Text("Recept", fontSize = 12.sp) }
        },
        fab = {
            FaltetFab(
                onClick = { editorTarget = EditorTarget(existing = null) },
                contentDescription = "Bygg bukett",
            )
        },
        watermark = BotanicalPlate.Harvest,
) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            uiState.bouquets.isEmpty() -> FaltetEmptyState(
                headline = "Inga buketter byggda än",
                subtitle = "Tryck på + för att binda en ny.",
                plate = app.verdant.android.ui.faltet.BotanicalPlate.Harvest,
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(uiState.bouquets, key = { it.id }) { b ->
                    FaltetListRow(
                        title = b.name,
                        meta = listOfNotNull<String>(
                            b.assembledAt.take(10),
                            b.sourceRecipeName?.let { "från $it" },
                            "${b.items.sumOf { it.stemCount }} st",
                        ).joinToString(" · "),
                        onClick = { editorTarget = EditorTarget(existing = b) },
                    )
                }
            }
        }
    }
}

private data class EditorTarget(val existing: BouquetResponse?)

private data class BouquetEditableItem(
    val speciesId: Long?,
    val stemCount: String,
    val role: String,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BouquetEditorDialog(
    existing: BouquetResponse?,
    recipes: List<BouquetRecipeResponse>,
    species: List<SpeciesResponse>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (sourceRecipeId: Long?, name: String, priceCents: Int?, items: List<CreateBouquetItemRequest>) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var sourceRecipeId by remember { mutableStateOf(existing?.sourceRecipeId) }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    // Phase 10 will source/edit price from the auto-created sale lot, not the
    // bouquet itself. For now seed from recipe only.
    var price by remember { mutableStateOf("") }
    val itemsState = remember {
        mutableStateListOf<BouquetEditableItem>().apply {
            existing?.items?.forEach { add(BouquetEditableItem(it.speciesId, it.stemCount.toString(), it.role)) }
        }
    }

    fun seedFromRecipe(recipe: BouquetRecipeResponse) {
        sourceRecipeId = recipe.id
        if (name.isBlank()) name = recipe.name
        if (price.isBlank()) recipe.priceCents?.let { price = (it / 100.0).toString() }
        itemsState.clear()
        recipe.items.forEach { itemsState.add(BouquetEditableItem(it.speciesId, it.stemCount.toString(), it.role)) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Redigera bukett" else "Bygg bukett") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (existing == null && recipes.isNotEmpty()) {
                    Text("Recept (valfritt)", fontSize = 12.sp, color = FaltetForest)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        recipes.forEach { r ->
                            FilterChip(
                                selected = r.id == sourceRecipeId,
                                onClick = { seedFromRecipe(r) },
                                label = { Text(r.name, fontSize = 12.sp) },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Namn") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Pris (SEK)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )

                Text("Stjälkar", fontWeight = FontWeight.Medium, fontSize = 13.sp)

                itemsState.forEachIndexed { index, item ->
                    BouquetItemEditor(
                        item = item,
                        species = species,
                        onChange = { itemsState[index] = it },
                        onRemove = { itemsState.removeAt(index) },
                    )
                }

                TextButton(onClick = { itemsState.add(BouquetEditableItem(null, "5", ItemRole.FLOWER)) }) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Lägg till stjälk")
                }

                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Radera bukett") }
                }
            }
        },
        confirmButton = {
            val valid = name.isNotBlank() && itemsState.isNotEmpty() && itemsState.all {
                it.speciesId != null && (it.stemCount.toIntOrNull() ?: 0) > 0
            }
            Button(
                enabled = valid && !saving,
                onClick = {
                    onSave(
                        sourceRecipeId,
                        name,
                        price.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toInt() },
                        itemsState.map {
                            CreateBouquetItemRequest(
                                speciesId = it.speciesId!!,
                                stemCount = it.stemCount.toInt(),
                                role = it.role,
                            )
                        },
                    )
                },
            ) { Text(if (saving) "Sparar…" else "Spara") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BouquetItemEditor(
    item: BouquetEditableItem,
    species: List<SpeciesResponse>,
    onChange: (BouquetEditableItem) -> Unit,
    onRemove: () -> Unit,
) {
    var speciesExpanded by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(
                    expanded = speciesExpanded,
                    onExpandedChange = { speciesExpanded = it },
                    modifier = Modifier.fillMaxWidth(0.85f),
                ) {
                    OutlinedTextField(
                        value = species.find { it.id == item.speciesId }?.let { it.commonNameSv ?: it.commonName } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Art") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryEditable, true),
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(speciesExpanded) },
                        singleLine = true,
                    )
                    DropdownMenu(
                        expanded = speciesExpanded,
                        onDismissRequest = { speciesExpanded = false },
                    ) {
                        species.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.commonNameSv ?: s.commonName) },
                                onClick = {
                                    onChange(item.copy(speciesId = s.id))
                                    speciesExpanded = false
                                },
                            )
                        }
                    }
                }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = null) }
            }

            OutlinedTextField(
                value = item.stemCount,
                onValueChange = { onChange(item.copy(stemCount = it.filter { c -> c.isDigit() })) },
                label = { Text("Antal") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ItemRole.values.forEach { r ->
                    FilterChip(
                        selected = r == item.role,
                        onClick = { onChange(item.copy(role = r)) },
                        label = { Text(roleLabel(r), fontSize = 12.sp) },
                    )
                }
            }
        }
    }
}

private fun roleLabel(role: String): String = when (role) {
    ItemRole.FLOWER -> "Blomma"
    ItemRole.FOLIAGE -> "Bladverk"
    ItemRole.FILLER -> "Fyllnad"
    ItemRole.ACCENT -> "Accent"
    else -> role
}

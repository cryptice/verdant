package app.verdant.android.ui.bouquet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.BouquetRecipeResponse
import app.verdant.android.data.model.CreateBouquetRecipeItemRequest
import app.verdant.android.data.model.CreateBouquetRecipeRequest
import app.verdant.android.data.model.ItemRole
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.UpdateBouquetRecipeRequest
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.theme.FaltetBlush
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetInk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BouquetState(
    val isLoading: Boolean = true,
    val items: List<BouquetRecipeResponse> = emptyList(),
    val species: List<SpeciesResponse> = emptyList(),
    val error: String? = null,
    val saving: Boolean = false,
)

@HiltViewModel
class BouquetViewModel @Inject constructor(
    private val repo: GardenRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BouquetState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val items = repo.getBouquetRecipes()
                val species = repo.getSpecies()
                _uiState.value = _uiState.value.copy(isLoading = false, items = items, species = species)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun create(request: CreateBouquetRecipeRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true)
            try {
                repo.createBouquetRecipe(request)
                _uiState.value = _uiState.value.copy(saving = false)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = e.message)
            }
        }
    }

    fun update(id: Long, request: UpdateBouquetRecipeRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true)
            try {
                repo.updateBouquetRecipe(id, request)
                _uiState.value = _uiState.value.copy(saving = false)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = e.message)
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            try {
                repo.deleteBouquetRecipe(id)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BouquetRecipesScreen(
    onBack: () -> Unit,
    viewModel: BouquetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BouquetRecipeResponse?>(null) }

    FaltetScreenScaffold(
        mastheadLeft = "§ Bukett",
        mastheadCenter = "Recept",
        fab = {
            FaltetFab(
                onClick = { editing = null; showDialog = true },
                contentDescription = stringResource(R.string.new_bouquet),
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))

            uiState.error != null && uiState.items.isEmpty() -> ConnectionErrorState(
                onRetry = { viewModel.refresh() },
                modifier = Modifier.padding(padding),
            )

            uiState.items.isEmpty() -> FaltetEmptyState(
                headline = "Inga recept",
                subtitle = "Designa din första bukett.",
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(uiState.items, key = { it.id }) { recipe ->
                    FaltetListRow(
                        title = recipe.name,
                        meta = "${recipe.items.sumOf { it.stemCount }} stjälkar",
                        leading = {
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                FaltetBlush.copy(alpha = 0.35f),
                                                FaltetBlush.copy(alpha = 0.12f),
                                                FaltetCream,
                                            ),
                                        ),
                                    )
                                    .border(1.dp, FaltetInk),
                            )
                        },
                        stat = recipe.priceSek?.let { price ->
                            {
                                Text(
                                    text = "$price KR",
                                    fontFamily = FaltetDisplay,
                                    fontSize = 16.sp,
                                    color = FaltetInk,
                                )
                            }
                        },
                        actions = null,
                        onClick = { editing = recipe; showDialog = true },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showDialog) {
        BouquetDialog(
            existing = editing,
            species = uiState.species,
            saving = uiState.saving,
            onDismiss = { showDialog = false },
            onSave = { name, priceSek, items ->
                if (editing != null) {
                    viewModel.update(
                        editing!!.id,
                        UpdateBouquetRecipeRequest(
                            name = name,
                            priceSek = priceSek,
                            items = items,
                        ),
                    )
                } else {
                    viewModel.create(
                        CreateBouquetRecipeRequest(
                            name = name,
                            priceSek = priceSek,
                            items = items,
                        ),
                    )
                }
                showDialog = false
            },
            onDelete = editing?.let { e ->
                { viewModel.delete(e.id); showDialog = false }
            },
        )
    }
}

@Composable
private fun roleLabel(role: String): String = when (role) {
    ItemRole.FLOWER -> stringResource(R.string.item_role_flower)
    ItemRole.FOLIAGE -> stringResource(R.string.item_role_foliage)
    ItemRole.FILLER -> stringResource(R.string.item_role_filler)
    ItemRole.ACCENT -> stringResource(R.string.item_role_accent)
    else -> role
}

private data class EditableItem(
    val speciesId: Long?,
    val stemCount: String,
    val role: String,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BouquetDialog(
    existing: BouquetRecipeResponse?,
    species: List<SpeciesResponse>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, priceSek: Int?, items: List<CreateBouquetRecipeItemRequest>) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var price by remember { mutableStateOf(existing?.priceSek?.toString() ?: "") }
    val itemsState = remember {
        mutableStateListOf<EditableItem>().apply {
            existing?.items?.forEach {
                add(EditableItem(it.speciesId, it.stemCount.toString(), it.role))
            }
            if (isEmpty()) add(EditableItem(null, "5", ItemRole.FLOWER))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing != null) R.string.edit_bouquet else R.string.new_bouquet)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.recipe_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.recipe_price)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )

                Text(stringResource(R.string.items), fontWeight = FontWeight.Medium, fontSize = 13.sp)

                itemsState.forEachIndexed { index, item ->
                    ItemEditor(
                        item = item,
                        species = species,
                        onChange = { itemsState[index] = it },
                        onRemove = { itemsState.removeAt(index) },
                    )
                }

                TextButton(onClick = { itemsState.add(EditableItem(null, "5", ItemRole.FLOWER)) }) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.items))
                }

                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.delete)) }
                }
            }
        },
        confirmButton = {
            val valid = name.isNotBlank() && itemsState.all {
                it.speciesId != null && it.stemCount.toIntOrNull() != null && it.stemCount.toInt() > 0
            }
            Button(
                enabled = valid && !saving,
                onClick = {
                    onSave(
                        name,
                        price.toIntOrNull(),
                        itemsState.map {
                            CreateBouquetRecipeItemRequest(
                                speciesId = it.speciesId!!,
                                stemCount = it.stemCount.toInt(),
                                role = it.role,
                            )
                        },
                    )
                },
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ItemEditor(
    item: EditableItem,
    species: List<SpeciesResponse>,
    onChange: (EditableItem) -> Unit,
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
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = species.find { it.id == item.speciesId }?.let { it.commonNameSv ?: it.commonName } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.species)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(speciesExpanded) },
                        singleLine = true,
                    )
                    ExposedDropdownMenu(
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
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }

            OutlinedTextField(
                value = item.stemCount,
                onValueChange = { onChange(item.copy(stemCount = it.filter { c -> c.isDigit() })) },
                label = { Text(stringResource(R.string.item_quantity)) },
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

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun BouquetRecipesScreenPreview() {
    FaltetScreenScaffold(
        mastheadLeft = "§ Bukett",
        mastheadCenter = "Recept",
    ) { padding ->
        FaltetEmptyState(
            headline = "Inga recept",
            subtitle = "Designa din första bukett.",
            modifier = Modifier.padding(padding),
        )
    }
}

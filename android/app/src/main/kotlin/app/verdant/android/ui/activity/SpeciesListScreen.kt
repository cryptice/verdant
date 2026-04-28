package app.verdant.android.ui.activity
import app.verdant.android.ui.faltet.BotanicalPlate
import app.verdant.android.data.repository.SpeciesRepository

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.SpeciesGroupRef
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.sortedBySwedishName
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSearchField
import app.verdant.android.ui.theme.FaltetBerry
import app.verdant.android.ui.theme.FaltetBlush
import app.verdant.android.ui.theme.FaltetButter
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetMustard
import app.verdant.android.ui.theme.FaltetSage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SpeciesListScreen"

private const val PAGE_SIZE = 50

data class SpeciesListState(
    val isLoading: Boolean = true,
    val species: List<SpeciesResponse> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class SpeciesListViewModel @Inject constructor(
    private val speciesRepository: SpeciesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SpeciesListState())
    val uiState = _uiState.asStateFlow()

    init { loadSpecies() }

    fun loadSpecies() {
        viewModelScope.launch {
            val showLoading = _uiState.value.species.isEmpty()
            if (showLoading) _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val species = speciesRepository.list().sortedBySwedishName()
                _uiState.value = SpeciesListState(isLoading = false, species = species)
            } catch (e: Exception) {
                if (showLoading) _uiState.value = SpeciesListState(isLoading = false, error = e.message)
            }
        }
    }

    fun deleteSpecies(id: Long) {
        viewModelScope.launch {
            try {
                speciesRepository.delete(id)
                _uiState.value = _uiState.value.copy(
                    species = _uiState.value.species.filter { it.id != id }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete species", e)
            }
        }
    }
}

private fun SpeciesResponse.displayName(): String {
    val name = commonNameSv ?: commonName
    val variant = variantNameSv ?: variantName
    return if (variant.isNullOrBlank()) name else "$name – $variant"
}

private fun SpeciesResponse.matchesQuery(query: String): Boolean {
    val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return true
    val haystack = listOfNotNull(
        commonName, commonNameSv, variantName, variantNameSv, scientificName,
    ).joinToString(" ").lowercase() +
        " " + groups.joinToString(" ") { it.name }.lowercase()
    return tokens.all { haystack.contains(it) }
}

private fun categoryColor(groups: List<SpeciesGroupRef>): Color {
    val name = groups.firstOrNull()?.name?.lowercase() ?: return FaltetAccent
    return when {
        name.contains("grönsak") || name.contains("veggie") || name.contains("vegetable") -> FaltetSage
        name.contains("snittblomma") || name.contains("snittblom") || name.contains("cut flower") -> FaltetBlush
        name.contains("ört") || name.contains("herb") -> FaltetButter
        name.contains("frukt") || name.contains("fruit") -> FaltetBerry
        name.contains("övrigt") || name.contains("other") -> FaltetMustard
        else -> FaltetAccent
    }
}

private fun categoryLabel(groups: List<SpeciesGroupRef>): String? =
    groups.firstOrNull()?.name

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeciesListScreen(
    onBack: () -> Unit,
    onAddSpecies: () -> Unit,
    onEditSpecies: (Long) -> Unit = {},
    onSowSpecies: (Long) -> Unit = {},
    viewModel: SpeciesListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var speciesToDelete by remember { mutableStateOf<SpeciesResponse?>(null) }

    LaunchedEffect(Unit) { viewModel.loadSpecies() }

    val filtered = remember(uiState.species, searchQuery) {
        uiState.species.filter { it.matchesQuery(searchQuery) }
    }

    if (speciesToDelete != null) {
        AlertDialog(
            onDismissRequest = { speciesToDelete = null },
            title = { Text("Ta bort") },
            text = { Text("Är du säker på att du vill ta bort den här sorten?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSpecies(speciesToDelete!!.id)
                    speciesToDelete = null
                }) { Text("Ta bort") }
            },
            dismissButton = {
                TextButton(onClick = { speciesToDelete = null }) { Text("Avbryt") }
            }
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Sorter",
        fab = { FaltetFab(onClick = onAddSpecies, contentDescription = "Lägg till sort") },
        watermark = BotanicalPlate.EmptyGarden,
) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.loadSpecies() })
            }
            filtered.isEmpty() && searchQuery.isBlank() -> FaltetEmptyState(
                headline = "Inga sorter",
                subtitle = "Lägg till en sort för att börja.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item {
                    FaltetSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "SÖK SORT",
                    )
                }
                items(filtered, key = { it.id }) { species ->
                    val dotColor = categoryColor(species.groups)
                    FaltetListRow(
                        title = species.displayName(),
                        meta = categoryLabel(species.groups),
                        leading = {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .drawBehind { drawCircle(dotColor) },
                            )
                        },
                        actions = null,
                        onClick = { onSowSpecies(species.id) },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun SpeciesListScreenPreview() {
    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Sorter",
        watermark = BotanicalPlate.EmptyGarden,
) { padding ->
        FaltetEmptyState(
            headline = "Inga sorter",
            subtitle = "Lägg till en sort för att börja.",
            modifier = Modifier.padding(padding),
        )
    }
}

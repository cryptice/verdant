package app.verdant.android.ui.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PAGE_SIZE = 50

data class SpeciesListState(
    val isLoading: Boolean = true,
    val species: List<SpeciesResponse> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class SpeciesListViewModel @Inject constructor(
    private val repo: GardenRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SpeciesListState())
    val uiState = _uiState.asStateFlow()

    init { loadSpecies() }

    fun loadSpecies() {
        viewModelScope.launch {
            val showLoading = _uiState.value.species.isEmpty()
            if (showLoading) _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val species = repo.getSpecies()
                _uiState.value = SpeciesListState(isLoading = false, species = species)
            } catch (e: Exception) {
                if (showLoading) _uiState.value = SpeciesListState(isLoading = false, error = e.message)
            }
        }
    }

    fun deleteSpecies(id: Long) {
        viewModelScope.launch {
            try {
                repo.deleteSpecies(id)
                _uiState.value = _uiState.value.copy(
                    species = _uiState.value.species.filter { it.id != id }
                )
            } catch (_: Exception) {}
        }
    }
}

private fun SpeciesResponse.displayName(): String {
    val name = commonNameSv ?: commonName
    val variant = variantNameSv ?: variantName
    return if (variant.isNullOrBlank()) name else "$name \u2013 $variant"
}

private fun SpeciesResponse.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.lowercase()
    return commonName.lowercase().contains(q) ||
        (commonNameSv?.lowercase()?.contains(q) == true) ||
        (variantName?.lowercase()?.contains(q) == true) ||
        (variantNameSv?.lowercase()?.contains(q) == true) ||
        (scientificName?.lowercase()?.contains(q) == true) ||
        (groupName?.lowercase()?.contains(q) == true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeciesListScreen(
    onBack: () -> Unit,
    onAddSpecies: () -> Unit,
    onEditSpecies: (Long) -> Unit = {},
    viewModel: SpeciesListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var speciesToDelete by remember { mutableStateOf<SpeciesResponse?>(null) }
    var page by remember { mutableStateOf(0) }

    // Reset page when search changes
    LaunchedEffect(searchQuery) { page = 0 }
    LaunchedEffect(Unit) { viewModel.loadSpecies() }

    val filtered = remember(uiState.species, searchQuery) {
        uiState.species.filter { it.matchesQuery(searchQuery) }
    }
    val totalPages = remember(filtered.size) { maxOf(1, (filtered.size + PAGE_SIZE - 1) / PAGE_SIZE) }
    val pagedItems = remember(filtered, page) {
        val start = page * PAGE_SIZE
        filtered.subList(start.coerceAtMost(filtered.size), (start + PAGE_SIZE).coerceAtMost(filtered.size))
    }

    // Autocomplete suggestions from all name fields
    val suggestions = remember(uiState.species, searchQuery) {
        if (searchQuery.length < 2) emptyList()
        else {
            val q = searchQuery.lowercase()
            uiState.species.flatMap { s ->
                listOfNotNull(s.commonName, s.commonNameSv, s.variantName, s.variantNameSv, s.scientificName)
            }.distinct().filter { it.lowercase().contains(q) }.take(5)
        }
    }
    var showSuggestions by remember { mutableStateOf(false) }

    if (speciesToDelete != null) {
        AlertDialog(
            onDismissRequest = { speciesToDelete = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_species_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSpecies(speciesToDelete!!.id)
                    speciesToDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { speciesToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.species)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSpecies) {
                Icon(Icons.Default.Add, stringResource(R.string.add_species))
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Search with autocomplete dropdown
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = showSuggestions && suggestions.isNotEmpty(),
                    onExpandedChange = { showSuggestions = it }
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it; showSuggestions = true },
                        placeholder = { Text(stringResource(R.string.search_species)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = showSuggestions && suggestions.isNotEmpty(),
                        onDismissRequest = { showSuggestions = false }
                    ) {
                        suggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    searchQuery = suggestion
                                    showSuggestions = false
                                }
                            )
                        }
                    }
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        app.verdant.android.ui.common.ConnectionErrorState(onRetry = { viewModel.loadSpecies() })
                    }
                }
                filtered.isEmpty() && searchQuery.isBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Spa, contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.no_species_found), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_species_found), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                else -> Column(Modifier.fillMaxSize()) {
                    // Result count
                    Text(
                        "${filtered.size} ${stringResource(R.string.species).lowercase()}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // Table header
                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.common_name_required).removeSuffix(" *"), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(150.dp))
                        Text(stringResource(R.string.scientific_name), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(150.dp))
                        Spacer(Modifier.width(40.dp)) // delete column
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Table body
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(pagedItems, key = { _, s -> s.id }) { index, species ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scrollState)
                                    .then(if (!species.isSystem) Modifier.clickable { onEditSpecies(species.id) } else Modifier)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    species.displayName(),
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(150.dp)
                                )
                                Text(
                                    species.scientificName ?: "",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(150.dp)
                                )
                                if (!species.isSystem) {
                                    IconButton(
                                        onClick = { speciesToDelete = species },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, stringResource(R.string.delete), modifier = Modifier.size(16.dp))
                                    }
                                } else {
                                    Spacer(Modifier.size(32.dp))
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }

                    // Pagination controls
                    if (totalPages > 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { page = (page - 1).coerceAtLeast(0) },
                                enabled = page > 0
                            ) { Text("<") }

                            Text(
                                "${page + 1} / $totalPages",
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            TextButton(
                                onClick = { page = (page + 1).coerceAtMost(totalPages - 1) },
                                enabled = page < totalPages - 1
                            ) { Text(">") }
                        }
                    }
                }
            }
        }
    }
}

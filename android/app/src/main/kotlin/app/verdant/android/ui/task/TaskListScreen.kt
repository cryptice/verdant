package app.verdant.android.ui.task

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.ScheduledTaskResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.theme.FaltetBerry
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetMustard
import app.verdant.android.ui.theme.FaltetSage
import app.verdant.android.ui.theme.FaltetSky
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

private const val TAG = "TaskListScreen"

data class TaskListState(
    val isLoading: Boolean = true,
    val tasks: List<ScheduledTaskResponse> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val repo: GardenRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TaskListState())
    val uiState = _uiState.asStateFlow()

    init { loadTasks() }

    fun loadTasks() {
        viewModelScope.launch {
            val showLoading = _uiState.value.tasks.isEmpty()
            if (showLoading) _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val tasks = repo.getTasks()
                _uiState.value = TaskListState(isLoading = false, tasks = tasks)
            } catch (e: Exception) {
                if (showLoading) _uiState.value = TaskListState(isLoading = false, error = e.message)
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            try {
                repo.deleteTask(taskId)
                _uiState.value = _uiState.value.copy(
                    tasks = _uiState.value.tasks.filter { it.id != taskId }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete task", e)
            }
        }
    }
}

private fun taskActivityLabel(activityType: String): String = when (activityType) {
    "SOW" -> "Så"
    "POT_UP" -> "Skola om"
    "PLANT" -> "Plantera"
    "HARVEST" -> "Skörda"
    "RECOVER" -> "Återhämta"
    "DISCARD" -> "Kassera"
    "WATER" -> "Vattna"
    "WEED" -> "Rensa ogräs"
    "FERTILIZE" -> "Gödsla"
    else -> activityType
}

private fun taskDotColor(activityType: String): Color = when (activityType) {
    "SOW" -> FaltetMustard
    "PLANT" -> FaltetSage
    "HARVEST" -> FaltetAccent
    "FERTILIZE" -> FaltetBerry
    else -> FaltetSky
}

private val svDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("sv"))

private fun formatDeadline(deadline: String): String = runCatching {
    LocalDate.parse(deadline).format(svDateFormatter)
}.getOrElse { deadline }

private data class TaskGroup(val label: String, val tasks: List<ScheduledTaskResponse>)

private fun groupTasks(tasks: List<ScheduledTaskResponse>): List<TaskGroup> {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val inSevenDays = today.plusDays(7)

    val overdue = mutableListOf<ScheduledTaskResponse>()
    val todayList = mutableListOf<ScheduledTaskResponse>()
    val tomorrowList = mutableListOf<ScheduledTaskResponse>()
    val thisWeek = mutableListOf<ScheduledTaskResponse>()
    val later = mutableListOf<ScheduledTaskResponse>()

    for (task in tasks) {
        val date = runCatching { LocalDate.parse(task.deadline) }.getOrNull()
        when {
            date == null -> later.add(task)
            date.isBefore(today) -> overdue.add(task)
            date.isEqual(today) -> todayList.add(task)
            date.isEqual(tomorrow) -> tomorrowList.add(task)
            date.isBefore(inSevenDays) -> thisWeek.add(task)
            else -> later.add(task)
        }
    }

    return listOf(
        TaskGroup("Förfallna", overdue),
        TaskGroup("Idag", todayList),
        TaskGroup("I morgon", tomorrowList),
        TaskGroup("Denna vecka", thisWeek),
        TaskGroup("Senare", later),
    ).filter { it.tasks.isNotEmpty() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onBack: () -> Unit,
    onCreateTask: () -> Unit,
    onEditTask: (Long) -> Unit,
    onPerformTask: (ScheduledTaskResponse) -> Unit,
    viewModel: TaskListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadTasks() }

    var taskToDelete by remember { mutableStateOf<ScheduledTaskResponse?>(null) }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Ta bort uppgift") },
            text = { Text("Vill du ta bort \"${task.originGroupName ?: task.speciesName ?: "Uppgift"}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(task.id)
                    taskToDelete = null
                }) { Text("Ta bort", color = FaltetClay) }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) { Text("Avbryt") }
            },
        )
    }

    val groups = remember(uiState.tasks) { groupTasks(uiState.tasks) }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Uppgifter",
        fab = { FaltetFab(onClick = onCreateTask, contentDescription = "Lägg till uppgift") },
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.loadTasks() })
            }
            uiState.tasks.isEmpty() -> FaltetEmptyState(
                headline = "Inga uppgifter",
                subtitle = "Skapa din första uppgift för säsongen.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                for (group in groups) {
                    item(key = "header_${group.label}") {
                        FaltetSectionHeader(label = group.label)
                    }
                    items(group.tasks, key = { it.id }) { task ->
                        val dotColor = taskDotColor(task.activityType)
                        val isBedTask = task.bedId != null
                        val title = when {
                            isBedTask -> taskActivityLabel(task.activityType)
                            else -> task.originGroupName ?: task.speciesName ?: "Uppgift"
                        }
                        val meta = buildString {
                            append(formatDeadline(task.deadline))
                            if (isBedTask) {
                                val bedLabel = listOfNotNull(task.gardenName, task.bedName).joinToString(" · ")
                                if (bedLabel.isNotBlank()) { append(" · "); append(bedLabel) }
                            } else {
                                val species = task.speciesName
                                if (species != null && species != title) {
                                    append(" · ")
                                    append(species)
                                }
                            }
                        }
                        FaltetListRow(
                            title = title,
                            meta = meta,
                            leading = {
                                androidx.compose.foundation.layout.Box(
                                    Modifier
                                        .size(10.dp)
                                        .drawBehind { drawCircle(dotColor) }
                                )
                            },
                            stat = null,
                            actions = {
                                IconButton(
                                    onClick = { taskToDelete = task },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Ta bort",
                                        tint = FaltetAccent,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            },
                            onClick = { onEditTask(task.id) },
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun TaskListScreenPreview() {
    val today = LocalDate.now()
    val tasks = listOf(
        FaltetListRow(
            title = "Ringblomma 'Indian Prince'",
            meta = "${today.minusDays(2).format(svDateFormatter)} · Calendula",
            leading = {
                androidx.compose.foundation.layout.Box(
                    Modifier.size(10.dp).drawBehind { drawCircle(FaltetMustard) }
                )
            },
            onClick = {},
        ),
        FaltetListRow(
            title = "Solros 'Vanilla Ice'",
            meta = "${today.format(svDateFormatter)} · Helianthus",
            leading = {
                androidx.compose.foundation.layout.Box(
                    Modifier.size(10.dp).drawBehind { drawCircle(FaltetSage) }
                )
            },
            onClick = {},
        ),
        FaltetListRow(
            title = "Dahlia 'Bishop of Llandaff'",
            meta = "${today.plusDays(3).format(svDateFormatter)} · Dahlia",
            leading = {
                androidx.compose.foundation.layout.Box(
                    Modifier.size(10.dp).drawBehind { drawCircle(FaltetAccent) }
                )
            },
            onClick = {},
        ),
    )

    LazyColumn {
        item { FaltetSectionHeader(label = "Förfallna") }
        item { tasks[0] }
        item { FaltetSectionHeader(label = "Idag") }
        item { tasks[1] }
        item { FaltetSectionHeader(label = "Denna vecka") }
        item { tasks[2] }
    }
}

package app.verdant.android.ui.task

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.ScheduledTaskResponse
import app.verdant.android.ui.theme.verdantTopAppBarColors
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

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
            } catch (_: Exception) {}
        }
    }
}

private fun activityIcon(type: String): ImageVector = when (type) {
    "SOW" -> Icons.Default.Grain
    "POT_UP" -> Icons.Default.Inventory2
    "PLANT" -> Icons.Default.Park
    "HARVEST" -> Icons.Default.Agriculture
    "RECOVER" -> Icons.Default.Shield
    "DISCARD" -> Icons.Default.Delete
    else -> Icons.Default.Task
}

private fun activityLabel(type: String): Int = when (type) {
    "SOW" -> R.string.activity_sow
    "POT_UP" -> R.string.activity_pot_up
    "PLANT" -> R.string.activity_plant
    "HARVEST" -> R.string.activity_harvest
    "RECOVER" -> R.string.activity_recover
    "DISCARD" -> R.string.activity_discard
    else -> R.string.task_activity_type
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
    val uiState by viewModel.uiState.collectAsState()
    var taskToDelete by remember { mutableStateOf<ScheduledTaskResponse?>(null) }

    // Refresh when returning from create/edit
    LaunchedEffect(Unit) { viewModel.loadTasks() }

    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text(stringResource(R.string.delete_task)) },
            text = { Text(stringResource(R.string.delete_task_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(taskToDelete!!.id)
                    taskToDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scheduled_tasks)) },
                colors = verdantTopAppBarColors()
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTask) {
                Icon(Icons.Default.Add, stringResource(R.string.create_task))
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    app.verdant.android.ui.common.ConnectionErrorState(onRetry = { viewModel.loadTasks() })
                }
            }
            uiState.tasks.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.no_tasks_yet), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(stringResource(R.string.tap_plus_to_create_task), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(uiState.tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onEditTask(task.id) },
                        onPerform = { onPerformTask(task) },
                        onDelete = { taskToDelete = task },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: ScheduledTaskResponse,
    onClick: () -> Unit,
    onPerform: () -> Unit,
    onDelete: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val deadline = remember(task.deadline) { LocalDate.parse(task.deadline) }
    val isOverdue = task.status == "PENDING" && deadline.isBefore(today)
    val isDueToday = task.status == "PENDING" && deadline.isEqual(today)
    val isCompleted = task.status == "COMPLETED"

    val cardAlpha = if (isCompleted) 0.6f else 1f

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    activityIcon(task.activityType),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = cardAlpha)
                )
                Text(
                    stringResource(activityLabel(task.activityType)),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = cardAlpha)
                )
                Spacer(Modifier.weight(1f))
                // Deadline badge
                val deadlineColor = when {
                    isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    isOverdue -> MaterialTheme.colorScheme.error
                    isDueToday -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
                val deadlineText = when {
                    isOverdue -> stringResource(R.string.task_overdue)
                    isDueToday -> stringResource(R.string.task_due_today)
                    else -> task.deadline
                }
                Text(deadlineText, fontSize = 12.sp, color = deadlineColor, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                task.speciesName,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha)
            )

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Progress
                val statusText = if (isCompleted) {
                    stringResource(R.string.task_completed)
                } else {
                    stringResource(R.string.task_remaining, task.remainingCount, task.targetCount)
                }
                Text(statusText, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!isCompleted) {
                        FilledTonalButton(
                            onClick = onPerform,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(stringResource(R.string.perform), fontSize = 13.sp)
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

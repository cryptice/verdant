package app.verdant.android.ui.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.verdant.android.ui.navigation.Screen
import app.verdant.android.ui.task.TaskFormScreen
import app.verdant.android.ui.task.TaskListScreen

fun NavGraphBuilder.taskGraph(navController: NavController) {
    composable(Screen.TaskList.route) {
        TaskListScreen(
            onBack = { navController.popBackStack() },
            onCreateTask = { navController.navigate(Screen.CreateTask.route) },
            onEditTask = { taskId -> navController.navigate(Screen.EditTask.create(taskId)) },
        )
    }
    composable(Screen.CreateTask.route) {
        TaskFormScreen(onBack = { navController.popBackStack() })
    }
    composable(
        Screen.EditTask.route,
        arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
    ) {
        TaskFormScreen(
            onBack = { navController.popBackStack() },
            onOpenRulePlace = { bedId, gardenAreaId ->
                when {
                    bedId != null -> navController.navigate(Screen.BedDetail.create(bedId))
                    gardenAreaId != null -> navController.navigate(Screen.GardenAreaDetail.create(gardenAreaId))
                }
            },
        )
    }
}

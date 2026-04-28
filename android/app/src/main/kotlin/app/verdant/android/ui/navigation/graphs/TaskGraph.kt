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
            onPerformTask = { task ->
                val speciesParam = task.speciesId?.let { "&speciesId=$it" } ?: ""
                when (task.activityType) {
                    "SOW" -> navController.navigate("activity/sow?taskId=${task.id}$speciesParam")
                    "POT_UP" -> navController.navigate(Screen.BatchPotUp.create(taskId = task.id, speciesId = task.speciesId))
                    "PLANT" -> navController.navigate("activity/plant-picker/SEEDED,POTTED_UP/plant-out?taskId=${task.id}$speciesParam")
                    "HARVEST" -> navController.navigate("activity/plant-picker/GROWING/harvest?taskId=${task.id}$speciesParam")
                    "RECOVER" -> navController.navigate("activity/plant-picker/GROWING/recover?taskId=${task.id}$speciesParam")
                    "DISCARD" -> navController.navigate("activity/plant-picker/SEEDED,POTTED_UP,PLANTED_OUT,GROWING,HARVESTED,RECOVERED/discard?taskId=${task.id}$speciesParam")
                    "WATER", "WEED", "FERTILIZE" -> task.bedId?.let { id ->
                        navController.navigate(Screen.BedDetail.create(id))
                    }
                }
            },
        )
    }
    composable(Screen.CreateTask.route) {
        TaskFormScreen(onBack = { navController.popBackStack() })
    }
    composable(
        Screen.EditTask.route,
        arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
    ) {
        TaskFormScreen(onBack = { navController.popBackStack() })
    }
}

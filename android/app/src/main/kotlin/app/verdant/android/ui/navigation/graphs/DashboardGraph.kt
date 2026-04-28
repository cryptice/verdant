package app.verdant.android.ui.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.verdant.android.ui.account.AccountScreen
import app.verdant.android.ui.dashboard.DashboardScreen
import app.verdant.android.ui.navigation.Screen
import app.verdant.android.ui.world.MyVerdantWorldScreen

fun NavGraphBuilder.dashboardGraph(navController: NavController) {
    composable(Screen.Dashboard.route) {
        DashboardScreen(
            onTaskClick = { task ->
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
                    else -> navController.navigate(Screen.TaskList.route)
                }
            },
            onOpenTasks = { navController.navigate(Screen.TaskList.route) },
            onSpeciesClick = { speciesId ->
                navController.navigate(Screen.PlantedSpeciesDetail.create(speciesId))
            },
            onOpenTrayLocation = { id ->
                navController.navigate(Screen.TrayLocationDetail.create(id))
            },
            onFertilizeTrayLocation = { id ->
                navController.navigate(Screen.ApplySupply.create(trayLocationId = id))
            },
        )
    }
    composable(Screen.MyWorld.route) {
        MyVerdantWorldScreen(
            onGardenClick = { gardenId -> navController.navigate(Screen.GardenDetail.create(gardenId)) },
            onCreateGarden = { navController.navigate(Screen.CreateGarden.route) },
            onSow = { navController.navigate(Screen.Sow.create()) },
            onSpeciesClick = { speciesId ->
                navController.navigate(Screen.PlantedSpeciesDetail.create(speciesId))
            },
        )
    }
    composable(Screen.Account.route) {
        AccountScreen(
            onSignedOut = { navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } } },
        )
    }
}

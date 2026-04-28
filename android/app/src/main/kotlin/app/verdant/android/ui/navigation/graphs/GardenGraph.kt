package app.verdant.android.ui.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.verdant.android.ui.activity.BatchPlantOutScreen
import app.verdant.android.ui.bed.BedDetailScreen
import app.verdant.android.ui.bed.CreateBedScreen
import app.verdant.android.ui.garden.CreateGardenScreen
import app.verdant.android.ui.garden.GardenDetailScreen
import app.verdant.android.ui.navigation.Screen
import app.verdant.android.ui.plant.CreatePlantScreen

fun NavGraphBuilder.gardenGraph(navController: NavController) {
    composable(Screen.CreateGarden.route) {
        CreateGardenScreen(
            onBack = { navController.popBackStack() },
            onCreated = { navController.navigate(Screen.MyWorld.route) { popUpTo(Screen.MyWorld.route) { inclusive = true } } },
        )
    }
    composable(
        Screen.GardenDetail.route,
        arguments = listOf(navArgument("gardenId") { type = NavType.LongType }),
    ) {
        GardenDetailScreen(
            onBack = { navController.popBackStack() },
            onBedClick = { bedId -> navController.navigate(Screen.BedDetail.create(bedId)) },
            onCreateBed = { gardenId -> navController.navigate(Screen.CreateBed.create(gardenId)) },
            onSpeciesClick = { speciesId ->
                navController.navigate(Screen.PlantedSpeciesDetail.create(speciesId))
            },
        )
    }
    composable(
        Screen.CreateBed.route,
        arguments = listOf(navArgument("gardenId") { type = NavType.LongType }),
    ) {
        CreateBedScreen(
            onBack = { navController.popBackStack() },
            onCreated = { _ -> navController.popBackStack() },
        )
    }
    composable(
        Screen.BedDetail.route,
        arguments = listOf(
            navArgument("bedId") { type = NavType.LongType },
            navArgument("edit") { type = NavType.BoolType; defaultValue = false },
        ),
    ) { backStackEntry ->
        val openEditOnStart = backStackEntry.arguments?.getBoolean("edit") ?: false
        BedDetailScreen(
            onBack = { navController.popBackStack() },
            onPlantClick = { plantId -> navController.navigate(Screen.PlantDetail.create(plantId)) },
            onSowInBed = { bedId -> navController.navigate(Screen.Sow.create(bedId = bedId)) },
            onPlantFromTray = { bedId -> navController.navigate(Screen.BatchPlantOut.create(bedId)) },
            onFertilize = { bedId -> navController.navigate(Screen.ApplySupply.create(bedId)) },
            onGardenClick = { gardenId -> navController.navigate(Screen.GardenDetail.create(gardenId)) },
            onBedCopied = { newBedId ->
                navController.navigate(Screen.BedDetail.create(newBedId, edit = true)) {
                    popUpTo(Screen.BedDetail.route) { inclusive = true }
                }
            },
            openEditOnStart = openEditOnStart,
        )
    }
    composable(
        Screen.CreatePlant.route,
        arguments = listOf(navArgument("bedId") { type = NavType.LongType }),
    ) {
        CreatePlantScreen(
            onBack = { navController.popBackStack() },
            onCreated = { navController.popBackStack() },
        )
    }
    composable(
        Screen.BatchPlantOut.route,
        arguments = listOf(navArgument("bedId") { type = NavType.LongType }),
    ) {
        BatchPlantOutScreen(onBack = { navController.popBackStack() })
    }
}

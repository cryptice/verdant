package app.verdant.android.ui.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.verdant.android.ui.navigation.Screen
import app.verdant.android.ui.sales.SaleLotDetailScreen
import app.verdant.android.ui.sales.SalesScreen

fun NavGraphBuilder.salesGraph(navController: NavController) {
    composable(Screen.Sales.route) {
        SalesScreen(onLotClick = { lotId -> navController.navigate(Screen.SaleLotDetail.create(lotId)) })
    }
    composable(
        Screen.SaleLotDetail.route,
        arguments = listOf(navArgument("lotId") { type = NavType.LongType }),
    ) {
        SaleLotDetailScreen(onBack = { navController.popBackStack() })
    }
}

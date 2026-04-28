package app.verdant.android.ui.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.verdant.android.ui.auth.AuthScreen
import app.verdant.android.ui.navigation.Screen
import app.verdant.android.ui.orgrequired.OrgRequiredScreen
import app.verdant.android.ui.splash.SplashScreen

fun NavGraphBuilder.authGraph(navController: NavController) {
    composable(Screen.Splash.route) {
        SplashScreen(
            onNavigateToAuth = { navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } } },
            onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) { popUpTo(0) { inclusive = true } } },
            onNavigateToOrgRequired = { navController.navigate(Screen.OrgRequired.route) { popUpTo(0) { inclusive = true } } },
        )
    }
    composable(Screen.Auth.route) {
        AuthScreen(
            onAuthSuccess = { navController.navigate(Screen.Dashboard.route) { popUpTo(0) { inclusive = true } } },
            onNeedsOrg = { navController.navigate(Screen.OrgRequired.route) { popUpTo(0) { inclusive = true } } },
        )
    }
    composable(Screen.OrgRequired.route) {
        OrgRequiredScreen(
            onOrgReady = { navController.navigate(Screen.Dashboard.route) { popUpTo(0) { inclusive = true } } },
            onSignedOut = { navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } } },
        )
    }
}

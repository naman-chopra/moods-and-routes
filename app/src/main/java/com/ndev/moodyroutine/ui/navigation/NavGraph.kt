package com.ndev.moodyroutine.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ndev.moodyroutine.ui.screens.home.HomeScreen
import com.ndev.moodyroutine.ui.screens.modes.ModeCreateScreen
import com.ndev.moodyroutine.ui.screens.modes.ModeDetailScreen
import com.ndev.moodyroutine.ui.screens.routines.RoutineCreateScreen
import com.ndev.moodyroutine.ui.screens.routines.RoutineDetailScreen
import com.ndev.moodyroutine.ui.screens.settings.SettingsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(
            route = Screen.ModeDetail.route,
            arguments = listOf(navArgument("modeId") { type = NavType.LongType })
        ) {
            ModeDetailScreen(navController = navController)
        }
        composable(
            route = Screen.ModeCreate.route,
            arguments = listOf(navArgument("modeId") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) {
            ModeCreateScreen(navController = navController)
        }
        composable(
            route = Screen.RoutineDetail.route,
            arguments = listOf(navArgument("routineId") { type = NavType.LongType })
        ) {
            RoutineDetailScreen(navController = navController)
        }
        composable(
            route = Screen.RoutineCreate.route,
            arguments = listOf(navArgument("routineId") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) {
            RoutineCreateScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}

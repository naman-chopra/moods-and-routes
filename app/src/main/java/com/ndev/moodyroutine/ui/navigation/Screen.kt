package com.ndev.moodyroutine.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ModeDetail : Screen("mode_detail/{modeId}") {
        fun createRoute(modeId: Long) = "mode_detail/$modeId"
    }
    object ModeCreate : Screen("mode_create?modeId={modeId}") {
        fun createRoute(modeId: Long? = null) = if (modeId != null) "mode_create?modeId=$modeId" else "mode_create"
    }
    object RoutineDetail : Screen("routine_detail/{routineId}") {
        fun createRoute(routineId: Long) = "routine_detail/$routineId"
    }
    object RoutineCreate : Screen("routine_create?routineId={routineId}") {
        fun createRoute(routineId: Long? = null) = if (routineId != null) "routine_create?routineId=$routineId" else "routine_create"
    }
    object Settings : Screen("settings")
    object DiagnosticLogs : Screen("diagnostic_logs")
}

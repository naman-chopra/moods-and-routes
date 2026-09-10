package com.ndev.moodyroutine.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PreferencesManager {

    private const val PREFS_NAME = "moody_routine_preferences"
    private const val KEY_DEBUG_LOGS_ENABLED = "debug_logs_enabled"
    private const val KEY_PERMISSION_ONBOARDING_COMPLETED = "permission_onboarding_completed"

    private val _isDebugLogsEnabled = MutableStateFlow(false)
    val isDebugLogsEnabled: StateFlow<Boolean> = _isDebugLogsEnabled.asStateFlow()

    private val _isPermissionOnboardingCompleted = MutableStateFlow(false)
    val isPermissionOnboardingCompleted: StateFlow<Boolean> = _isPermissionOnboardingCompleted.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun init(context: Context) {
        val prefs = getPrefs(context)
        val enabled = prefs.getBoolean(KEY_DEBUG_LOGS_ENABLED, false)
        _isDebugLogsEnabled.value = enabled
        AppLogger.setLoggingEnabled(enabled)
        _isPermissionOnboardingCompleted.value = prefs.getBoolean(KEY_PERMISSION_ONBOARDING_COMPLETED, false)
    }

    fun setDebugLogsEnabled(context: Context, enabled: Boolean) {
        _isDebugLogsEnabled.value = enabled
        getPrefs(context).edit().putBoolean(KEY_DEBUG_LOGS_ENABLED, enabled).apply()
        AppLogger.setLoggingEnabled(enabled)
    }

    fun isDebugLogsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DEBUG_LOGS_ENABLED, false)
    }

    fun setPermissionOnboardingCompleted(context: Context, completed: Boolean) {
        _isPermissionOnboardingCompleted.value = completed
        getPrefs(context).edit().putBoolean(KEY_PERMISSION_ONBOARDING_COMPLETED, completed).apply()
    }

    fun isPermissionOnboardingCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PERMISSION_ONBOARDING_COMPLETED, false)
    }
}

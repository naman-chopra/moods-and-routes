package com.ndev.moodyroutine

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.ndev.moodyroutine.ui.dialogs.PermissionOnboardingDialog
import com.ndev.moodyroutine.ui.navigation.NavGraph
import com.ndev.moodyroutine.ui.theme.MoodyRoutineTheme
import com.ndev.moodyroutine.util.PreferencesManager

class MainActivity : ComponentActivity() {

    private fun startAutomationService() {
        val serviceIntent = Intent(this, com.ndev.moodyroutine.service.AutomationService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (_: Exception) {}
    }

    private fun hasAllEssentialPermissions(): Boolean {
        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        val loc = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batt = pm?.isIgnoringBatteryOptimizations(packageName) == true

        return notif && loc && batt
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val isOnboardingCompletedInitial = PreferencesManager.isPermissionOnboardingCompleted(this) || hasAllEssentialPermissions()
        if (isOnboardingCompletedInitial) {
            PreferencesManager.setPermissionOnboardingCompleted(this, true)
            startAutomationService()
        }

        enableEdgeToEdge()
        setContent {
            MoodyRoutineTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showOnboarding by remember { mutableStateOf(!PreferencesManager.isPermissionOnboardingCompleted(this)) }

                    val navController = rememberNavController()
                    NavGraph(navController = navController)

                    if (showOnboarding) {
                        PermissionOnboardingDialog(
                            onDismiss = {
                                PreferencesManager.setPermissionOnboardingCompleted(this@MainActivity, true)
                                showOnboarding = false
                                startAutomationService()
                            },
                            onComplete = {
                                PreferencesManager.setPermissionOnboardingCompleted(this@MainActivity, true)
                                showOnboarding = false
                                startAutomationService()
                            }
                        )
                    }
                }
            }
        }
    }
}

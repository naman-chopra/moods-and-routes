package com.ndev.moodyroutine.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ndev.moodyroutine.service.AutomationService
import com.ndev.moodyroutine.ui.theme.MoodyRoutineTheme

class AppBlockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_MODE_NAME = "extra_mode_name"
        const val EXTRA_MODE_ID = "extra_mode_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "This app"
        val modeName = intent.getStringExtra(EXTRA_MODE_NAME) ?: "an active mode"
        val modeId = intent.getLongExtra(EXTRA_MODE_ID, -1L)
        val pkgName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""

        setContent {
            MoodyRoutineTheme {
                AppBlockScreen(
                    appName = appName,
                    modeName = modeName,
                    onStayFocused = {
                        returnToHome()
                    },
                    onSnooze = {
                        // Temporarily allow this app for 5 minutes
                        if (pkgName.isNotBlank()) {
                            AutomationService.temporarilyAllowApp(pkgName, 5 * 60 * 1000L)
                        }
                        finish()
                    },
                    onTurnOffMode = {
                        if (modeId != -1L) {
                            val turnOffIntent = Intent(this, AutomationService::class.java).apply {
                                action = AutomationService.ACTION_TURN_OFF_MODE
                                putExtra(AutomationService.EXTRA_MODE_ID, modeId)
                            }
                            startService(turnOffIntent)
                        }
                        finish()
                    }
                )
            }
        }
    }

    private fun returnToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        returnToHome()
    }
}

@Composable
fun AppBlockScreen(
    appName: String,
    modeName: String,
    onStayFocused: () -> Unit,
    onSnooze: () -> Unit,
    onTurnOffMode: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Stay focused",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$appName is restricted while $modeName mode is on.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onStayFocused,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Close $appName", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) {
                Icon(Icons.Rounded.HourglassTop, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Use for 5 minutes", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onTurnOffMode,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Turn off $modeName mode", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

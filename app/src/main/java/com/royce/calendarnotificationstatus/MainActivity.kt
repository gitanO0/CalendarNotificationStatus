package com.royce.calendarnotificationstatus

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.royce.calendarnotificationstatus.ui.theme.CalendarNotificationStatusTheme

class MainActivity : ComponentActivity() {

    private val permissionsToRequest = mutableListOf<String>().apply {
        add(Manifest.permission.READ_CALENDAR)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var allGranted = true
        permissions.entries.forEach {
            if (!it.value) allGranted = false
        }
        if (allGranted) {
            NotificationUpdater.updateNotification(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)

        setContent {
            CalendarNotificationStatusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var isEnabled by remember { 
                        mutableStateOf(prefs.getBoolean("notification_enabled", false)) 
                    }
                    var permissionsGranted by remember {
                        mutableStateOf(checkPermissions())
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Calendar Notification Status",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "Version: v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))

                        if (!permissionsGranted) {
                            Text(
                                text = "App requires Calendar and Notification permissions to function properly.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                requestPermissionLauncher.launch(permissionsToRequest)
                                // We check again immediately, but the real update happens in the launcher callback
                                permissionsGranted = checkPermissions()
                            }) {
                                Text("Grant Permissions")
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Enable Persistent Notification",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        isEnabled = checked
                                        prefs.edit().putBoolean("notification_enabled", checked).apply()
                                        NotificationUpdater.updateNotification(this@MainActivity)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (checkPermissions()) {
            NotificationUpdater.updateNotification(this)
        }
    }

    private fun checkPermissions(): Boolean {
        var granted = true
        for (permission in permissionsToRequest) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                granted = false
            }
        }
        return granted
    }
}

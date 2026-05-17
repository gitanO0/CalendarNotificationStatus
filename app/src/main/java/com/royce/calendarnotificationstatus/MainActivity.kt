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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
                    var availableCalendars by remember {
                        mutableStateOf<List<CalendarInfo>>(emptyList())
                    }
                    var selectedCalendarIds by remember {
                        mutableStateOf(prefs.getStringSet("selected_calendars", emptySet()) ?: emptySet())
                    }

                    LaunchedEffect(permissionsGranted) {
                        if (permissionsGranted) {
                            availableCalendars = CalendarHelper.getAvailableCalendars(this@MainActivity)
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Top-left version text
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(top = 16.dp, start = 16.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .padding(top = 32.dp), // Add some top padding so content doesn't overlap the absolute version text
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Calendar Notification Status",
                                style = MaterialTheme.typography.headlineMedium,
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
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                Text(
                                    text = "Synced Calendars",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Select which calendars to display. If none are selected, all visible calendars will be shown.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                ) {
                                    items(availableCalendars) { calendar ->
                                        val isSelected = selectedCalendarIds.contains(calendar.id.toString())
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    val newSet = selectedCalendarIds.toMutableSet()
                                                    if (checked) {
                                                        newSet.add(calendar.id.toString())
                                                    } else {
                                                        newSet.remove(calendar.id.toString())
                                                    }
                                                    selectedCalendarIds = newSet
                                                    prefs.edit().putStringSet("selected_calendars", newSet).apply()
                                                    if (isEnabled) {
                                                        NotificationUpdater.updateNotification(this@MainActivity)
                                                    }
                                                }
                                            )
                                            Column {
                                                Text(text = calendar.displayName, style = MaterialTheme.typography.bodyLarge)
                                                Text(text = calendar.accountName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                    }
                                }
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

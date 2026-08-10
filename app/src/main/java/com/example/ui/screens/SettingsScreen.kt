package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean = true,
    onDarkThemeToggle: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: MainViewModel? = null
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var emailAlertsEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Settings, contentDescription = null, tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "App Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_screen"),
            contentPadding = PaddingValues(16.dp, bottom = 80.dp)
        ) {
            item {
                Text(
                    text = "App Preferences & Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Theme Setting
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Theme Mode", fontWeight = FontWeight.Bold)
                            Text("Toggle high-contrast eye-safe dark theme", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onDarkThemeToggle() },
                            modifier = Modifier.testTag("settings_dark_mode_switch")
                        )
                    }
                }

                // Push Notifications
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Push Notifications", fontWeight = FontWeight.Bold)
                            Text("Receive instant alerts for note sales & purchase downloads", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            modifier = Modifier.testTag("settings_notifications_switch")
                        )
                    }
                }

                // Email Alerts
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Email Alerts & Receipts", fontWeight = FontWeight.Bold)
                            Text("Get Instamojo transaction receipts via email", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = emailAlertsEnabled,
                            onCheckedChange = { emailAlertsEnabled = it },
                            modifier = Modifier.testTag("settings_email_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Application Information", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("App Name: StudySwap AI", fontSize = 12.sp)
                        Text("Version: 2.4.0 Production Build (STUDYVERSE_BUILD_DIAGNOSTIC = 2.4.1)", fontSize = 12.sp)
                        Text("Build Engine: Jetpack Compose + Room + Instamojo + Gemini AI", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                if (viewModel != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("developer_diagnostics_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Developer Firebase Diagnostics", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            var diagnosticOutput by remember { mutableStateOf("") }
                            var isTesting by remember { mutableStateOf(false) }

                            if (diagnosticOutput.isNotEmpty()) {
                                Text(diagnosticOutput, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Button(
                                onClick = {
                                    isTesting = true
                                    viewModel.runFirebaseDiagnostics { result ->
                                        diagnosticOutput = result
                                        isTesting = false
                                    }
                                },
                                enabled = !isTesting,
                                modifier = Modifier.fillMaxWidth().testTag("run_diagnostics_button")
                            ) {
                                Text(if (isTesting) "Running Diagnostics..." else "Run Firebase Diagnostics")
                            }
                        }
                    }
                }
            }
        }
    }
}

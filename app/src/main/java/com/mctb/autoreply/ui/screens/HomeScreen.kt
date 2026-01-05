package com.mctb.autoreply.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mctb.autoreply.R
import com.mctb.autoreply.data.AppPreferences
import com.mctb.autoreply.service.CallMonitorService
import com.mctb.autoreply.util.SmsHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()

    // Collect state from DataStore
    val isEnabled by prefs.isEnabled.collectAsState(initial = true)
    val isAlwaysOn by prefs.isAlwaysOn.collectAsState(initial = true)
    val usageStatus by prefs.usageStatus.collectAsState(initial = com.mctb.autoreply.data.UsageStatus(0, false))

    // Permission handling
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.SEND_SMS,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.SEND_SMS
        )
    }

    val permissionsState = rememberMultiplePermissionsState(permissions = permissions)

    var showLimitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topAppBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission warning card
            if (!permissionsState.allPermissionsGranted) {
                PermissionWarningCard(permissionsState)
            }

            // Status card
            StatusCard(isEnabled = isEnabled, isAlwaysOn = isAlwaysOn)

            // Master enable/disable switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.enable_auto_reply),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                // Check permissions
                                if (!permissionsState.allPermissionsGranted) {
                                    permissionsState.launchMultiplePermissionRequest()
                                    return@Switch
                                }

                                // Check limit
                                if (usageStatus.hasReachedLimit) {
                                    showLimitDialog = true
                                    return@Switch
                                }

                                // Enable
                                scope.launch {
                                    prefs.setEnabled(true)
                                    CallMonitorService.start(context)
                                }
                            } else {
                                // Disable
                                scope.launch {
                                    prefs.setEnabled(false)
                                    CallMonitorService.stop(context)
                                }
                            }
                        }
                    )
                }
            }

            // Usage display
            UsageCard(usageStatus = usageStatus, onViewUsageClick = {
                navController.navigate("usage")
            })

            // Configuration buttons
            ConfigurationButtons(
                onMessageEditorClick = { navController.navigate("message_editor") },
                onActiveHoursClick = { navController.navigate("active_hours") }
            )

            // Debug Section
            DebugCard(prefs = prefs, scope = scope, context = context)

            // Battery optimization reminder
            if (isEnabled) {
                BatteryOptimizationCard()
            }
        }
    }

    // Limit reached dialog
    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text(stringResource(R.string.limit_reached_title)) },
            text = { Text(stringResource(R.string.limit_reached_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    showLimitDialog = false
                    navController.navigate("usage")
                }) {
                    Text(stringResource(R.string.upgrade_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun StatusCard(isEnabled: Boolean, isAlwaysOn: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isEnabled) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isEnabled)
                    MaterialTheme.colorScheme.onTertiaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = when {
                    !isEnabled -> stringResource(R.string.status_inactive)
                    isAlwaysOn -> stringResource(R.string.status_active_24_7)
                    else -> stringResource(R.string.status_active_hours)
                },
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun UsageCard(usageStatus: com.mctb.autoreply.data.UsageStatus, onViewUsageClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.usage_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (usageStatus.isUnlimited) {
                Text(
                    text = stringResource(R.string.usage_unlimited),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.usage_count,
                        usageStatus.count,
                        AppPreferences.FREE_TIER_LIMIT
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { usageStatus.count.toFloat() / AppPreferences.FREE_TIER_LIMIT },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onViewUsageClick) {
                Text(stringResource(R.string.view_usage))
            }
        }
    }
}

@Composable
fun ConfigurationButtons(
    onMessageEditorClick: () -> Unit,
    onActiveHoursClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = onMessageEditorClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(modifier = Modifier.padding(4.dp))
            Text(stringResource(R.string.configure_message))
        }

        FilledTonalButton(
            onClick = onActiveHoursClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Schedule, contentDescription = null)
            Spacer(modifier = Modifier.padding(4.dp))
            Text(stringResource(R.string.configure_hours))
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionWarningCard(permissionsState: MultiplePermissionsState) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = stringResource(R.string.permissions_required_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.permissions_required_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(
                onClick = { permissionsState.launchMultiplePermissionRequest() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.grant_permissions))
            }
        }
    }
}

@Composable
fun BatteryOptimizationCard() {
    val context = LocalContext.current

    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.battery_optimization_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.battery_optimization_desc),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Battery optimization settings not available
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.disable_optimization))
            }
        }
    }
}

@Composable
fun DebugCard(prefs: AppPreferences, scope: CoroutineScope, context: android.content.Context) {
    var testStatus by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Debug & Testing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (testStatus.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = testStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FilledTonalButton(
                onClick = {
                    isLoading = true
                    testStatus = "Testing SMS functionality..."
                    Log.i("DebugCard", "=== MANUAL TEST STARTED ===")

                    scope.launch {
                        try {
                            // Check all settings
                            val enabled = prefs.isEnabledSync()
                            val alwaysOn = prefs.isAlwaysOnSync()
                            val message = prefs.getMessageSync()
                            val count = prefs.getAutoTextCountSync()
                            val unlimited = prefs.isUnlimitedSync()

                            Log.i("DebugCard", "Enabled: $enabled")
                            Log.i("DebugCard", "AlwaysOn: $alwaysOn")
                            Log.i("DebugCard", "Message: $message")
                            Log.i("DebugCard", "Count: $count")
                            Log.i("DebugCard", "Unlimited: $unlimited")

                            testStatus = "Enabled: $enabled\nAlwaysOn: $alwaysOn\nMessage length: ${message.length}\nCount: $count/$5"

                            // Test SMS handler with a fake number
                            val smsHandler = SmsHandler(context)
                            val testNumber = "1234567890"
                            Log.i("DebugCard", "Testing SMS to: $testNumber")

                            val result = smsHandler.processMissedCall(testNumber)

                            Log.i("DebugCard", "=== TEST RESULT: $result ===")

                            testStatus = if (result) {
                                "✅ TEST PASSED!\nSMS sent successfully\nCheck logs for details"
                            } else {
                                "❌ TEST FAILED\nSMS was not sent\nCheck logs for reason"
                            }

                            Toast.makeText(
                                context,
                                if (result) "Test SMS sent!" else "Test failed - check logs",
                                Toast.LENGTH_LONG
                            ).show()

                        } catch (e: Exception) {
                            Log.e("DebugCard", "Test error", e)
                            testStatus = "❌ ERROR: ${e.message}"
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text(if (isLoading) "Running Test..." else "Test SMS Sending")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Note: Check logcat with tag 'DebugCard', 'SmsHandler', and 'CallReceiver' for detailed info",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

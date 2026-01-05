package com.mctb.autoreply.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.mctb.autoreply.R
import com.mctb.autoreply.data.AppPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveHoursScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val startHour by prefs.startHour.collectAsState(initial = AppPreferences.DEFAULT_START_HOUR)
    val startMinute by prefs.startMinute.collectAsState(initial = AppPreferences.DEFAULT_START_MINUTE)
    val endHour by prefs.endHour.collectAsState(initial = AppPreferences.DEFAULT_END_HOUR)
    val endMinute by prefs.endMinute.collectAsState(initial = AppPreferences.DEFAULT_END_MINUTE)
    val isAlwaysOn by prefs.isAlwaysOn.collectAsState(initial = true)

    var localAlwaysOn by remember { mutableStateOf(isAlwaysOn) }
    var localStartHour by remember { mutableStateOf(startHour) }
    var localStartMinute by remember { mutableStateOf(startMinute) }
    var localEndHour by remember { mutableStateOf(endHour) }
    var localEndMinute by remember { mutableStateOf(endMinute) }

    // Sync local state with Flow values
    LaunchedEffect(isAlwaysOn) { localAlwaysOn = isAlwaysOn }
    LaunchedEffect(startHour) { localStartHour = startHour }
    LaunchedEffect(startMinute) { localStartMinute = startMinute }
    LaunchedEffect(endHour) { localEndHour = endHour }
    LaunchedEffect(endMinute) { localEndMinute = endMinute }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topAppBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.active_hours_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Always On switch
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.always_active),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.always_active_desc),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = localAlwaysOn,
                        onCheckedChange = { localAlwaysOn = it }
                    )
                }
            }

            // Time selection (disabled when always on)
            if (!localAlwaysOn) {
                Text(
                    text = stringResource(R.string.active_hours_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Start time
                Column {
                    Text(
                        text = stringResource(R.string.start_time),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.time_format, localStartHour, localStartMinute),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                // End time
                Column {
                    Text(
                        text = stringResource(R.string.end_time),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.time_format, localEndHour, localEndMinute),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    scope.launch {
                        prefs.setAlwaysOn(localAlwaysOn)
                        if (!localAlwaysOn) {
                            prefs.setActiveHours(
                                localStartHour,
                                localStartMinute,
                                localEndHour,
                                localEndMinute
                            )
                        }
                        snackbarHostState.showSnackbar(context.getString(R.string.hours_saved))
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }

    // Time picker dialogs
    if (showStartTimePicker) {
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                localStartHour = hour
                localStartMinute = minute
                showStartTimePicker = false
            },
            initialHour = localStartHour,
            initialMinute = localStartMinute
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                localEndHour = hour
                localEndMinute = minute
                showEndTimePicker = false
            },
            initialHour = localEndHour,
            initialMinute = localEndMinute
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    initialHour: Int,
    initialMinute: Int
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.select_time),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(state = timePickerState)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    androidx.compose.material3.TextButton(
                        onClick = {
                            onConfirm(timePickerState.hour, timePickerState.minute)
                        }
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                }
            }
        }
    }
}

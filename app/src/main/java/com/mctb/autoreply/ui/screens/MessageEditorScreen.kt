package com.mctb.autoreply.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mctb.autoreply.R
import com.mctb.autoreply.data.AppPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageEditorScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentMessage by prefs.message.collectAsState(initial = AppPreferences.DEFAULT_MESSAGE)
    var messageText by remember { mutableStateOf(currentMessage) }

    // Update local state when DataStore value changes
    if (messageText != currentMessage && messageText == AppPreferences.DEFAULT_MESSAGE) {
        messageText = currentMessage
    }

    val maxLength = 160
    val charCount = messageText.length

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.message_editor_title)) },
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
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.message_label),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = messageText,
                onValueChange = {
                    if (it.length <= maxLength) {
                        messageText = it
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                placeholder = { Text(stringResource(R.string.message_hint)) },
                supportingText = {
                    Text(
                        text = stringResource(R.string.char_count, charCount),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                isError = charCount == 0,
                maxLines = 6
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (messageText.isNotBlank()) {
                        scope.launch {
                            prefs.setMessage(messageText)
                            snackbarHostState.showSnackbar(context.getString(R.string.message_saved))
                            navController.popBackStack()
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.message_empty_error))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = messageText.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    messageText = AppPreferences.DEFAULT_MESSAGE
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.reset_default))
            }
        }
    }
}

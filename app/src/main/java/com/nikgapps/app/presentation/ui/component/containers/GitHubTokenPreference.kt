package com.nikgapps.app.presentation.ui.component.containers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nikgapps.app.data.GithubPrefs
import com.nikgapps.app.presentation.ui.component.items.PreferenceItem
import com.nikgapps.app.utils.network.GitHubDeviceAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun GitHubTokenPreference(asSignInButton: Boolean = false) {
    var dialogOpen by remember { mutableStateOf(false) }
    if (asSignInButton) {
        OutlinedButton(onClick = { dialogOpen = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Use a personal access token")
        }
    } else {
        PreferenceItem(
            label = "GitHub token",
            supportingText = if (GithubPrefs.token.isBlank()) "Not configured" else "Connected as ${GithubPrefs.username}",
            icon = Icons.Outlined.Key,
            onClick = { dialogOpen = true }
        )
    }

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = { Text("Sign in with a GitHub token") },
            text = { GitHubTokenEntry(onSignedIn = { dialogOpen = false }) },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun GitHubTokenEntry(onSignedIn: () -> Unit = {}, showExplanation: Boolean = true) {
    var token by remember { mutableStateOf("") }
    var verifying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val fieldBringIntoView = remember { BringIntoViewRequester() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showExplanation) {
            Text("Paste a GitHub personal access token. NikGapps will verify it before continuing.")
        }
        OutlinedTextField(
            value = token,
            onValueChange = { token = it; error = null },
            label = { Text("Personal access token") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
                .bringIntoViewRequester(fieldBringIntoView)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        scope.launch {
                            delay(300)
                            fieldBringIntoView.bringIntoView()
                        }
                    }
                }
        )
        error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                val candidate = token.trim()
                verifying = true
                error = null
                scope.launch {
                    try {
                        val login = GitHubDeviceAuth.account(candidate)
                        GithubPrefs.token = candidate
                        GithubPrefs.username = login
                        token = ""
                        onSignedIn()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        error = failure.message ?: "Could not verify this GitHub token"
                    } finally {
                        verifying = false
                    }
                }
            },
            enabled = token.trim().isNotEmpty() && !verifying,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (verifying) CircularProgressIndicator(modifier = Modifier.size(18.dp).padding(end = 2.dp), strokeWidth = 2.dp)
            Text("Verify and continue")
        }
    }
}

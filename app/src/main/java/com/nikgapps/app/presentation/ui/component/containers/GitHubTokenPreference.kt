package com.nikgapps.app.presentation.ui.component.containers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nikgapps.app.data.GithubPrefs
import com.nikgapps.app.presentation.ui.component.items.PreferenceItem
import com.nikgapps.app.utils.network.GitHubDeviceAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun GitHubTokenPreference() {
    var dialogOpen by remember { mutableStateOf(false) }
    PreferenceItem(
        label = "GitHub token",
        supportingText = if (GithubPrefs.token.isBlank()) "Not configured" else "Connected as ${GithubPrefs.username}",
        icon = Icons.Outlined.Key,
        onClick = { dialogOpen = true }
    )

    if (dialogOpen) {
        GitHubTokenTopSheet(
            onDismiss = { dialogOpen = false },
            onSaved = { dialogOpen = false }
        )
    }
}

@Composable
private fun GitHubTokenTopSheet(onDismiss: () -> Unit, onSaved: () -> Unit) {
    var sheetVisible by remember { mutableStateOf(false) }
    val fieldFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        sheetVisible = true
        delay(120)
        fieldFocusRequester.requestFocus()
        keyboard?.show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            AnimatedVisibility(
                visible = sheetVisible,
                enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
                    .statusBarsPadding().navigationBarsPadding().imePadding()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("GitHub token", style = MaterialTheme.typography.headlineSmall)
                        GitHubTokenEntry(onSaved = onSaved, focusRequester = fieldFocusRequester)
                    }
                }
            }
        }
    }
}

@Composable
private fun GitHubTokenEntry(onSaved: () -> Unit, focusRequester: FocusRequester) {
    var token by remember { mutableStateOf(GithubPrefs.token) }
    var verifying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Paste a token to connect. Clear the field and save to sign out.")
        OutlinedTextField(
            value = token,
            onValueChange = { token = it; error = null },
            label = { Text("Personal access token") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
        )
        error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                val candidate = token.trim()
                error = null
                if (candidate.isEmpty()) {
                    if (GithubPrefs.username.isNotBlank()) GithubPrefs.lastUsername = GithubPrefs.username
                    GithubPrefs.token = ""
                    GithubPrefs.username = ""
                    GithubPrefs.avatarUrl = ""
                    onSaved()
                    return@Button
                }
                verifying = true
                scope.launch {
                    try {
                        val profile = GitHubDeviceAuth.accountProfile(candidate)
                        GithubPrefs.token = candidate
                        GithubPrefs.username = profile.login
                        GithubPrefs.lastUsername = profile.login
                        GithubPrefs.avatarUrl = profile.avatarUrl
                        token = ""
                        onSaved()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        error = failure.message ?: "Could not verify this GitHub token"
                    } finally {
                        verifying = false
                    }
                }
            },
            enabled = !verifying,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (verifying) CircularProgressIndicator(modifier = Modifier.size(18.dp).padding(end = 2.dp), strokeWidth = 2.dp)
            Text("Save token")
        }
    }
}

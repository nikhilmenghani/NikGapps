package com.nikgapps.app.presentation.ui.component.containers

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.nikgapps.R
import com.nikgapps.BuildConfig
import com.nikgapps.app.data.GithubPrefs
import com.nikgapps.app.presentation.ui.component.items.PreferenceItem
import com.nikgapps.app.utils.network.GitHubDeviceAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun GitHubAccountPreference(asSignInButton: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dialogOpen by remember { mutableStateOf(false) }
    var challenge by remember { mutableStateOf<GitHubDeviceAuth.Challenge?>(null) }
    var finishingSignIn by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val clientId = BuildConfig.GITHUB_CLIENT_ID

    if (asSignInButton) {
        Button(
            onClick = { dialogOpen = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF24292F),
                contentColor = Color.White
            )
        ) {
            Icon(painterResource(R.drawable.ic_github_mark), contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Continue with GitHub")
        }
    } else PreferenceItem(
        label = "GitHub account",
        supportingText = when {
            GithubPrefs.username.isNotBlank() -> "Signed in as ${GithubPrefs.username}"
            GithubPrefs.token.isNotBlank() -> "Personal access token configured"
            clientId.isBlank() -> "Sign-in unavailable in this build; manual token is available"
            else -> "Sign in with a code to connect your account"
        },
        icon = Icons.Outlined.AccountCircle,
        onClick = { dialogOpen = true }
    )

    if (dialogOpen) {
        LaunchedEffect(challenge) {
            val active = challenge ?: return@LaunchedEffect
            try {
                val token = GitHubDeviceAuth.awaitToken(clientId, active)
                if (GithubPrefs.username.isBlank()) GithubPrefs.token = token
                finishingSignIn = true
                val profile = GitHubDeviceAuth.accountProfileWithRetry(token)
                GithubPrefs.token = token
                GithubPrefs.username = profile.login
                GithubPrefs.avatarUrl = profile.avatarUrl
                dialogOpen = false
                challenge = null
                Toast.makeText(context, "Signed in to GitHub as ${profile.login}", Toast.LENGTH_SHORT).show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "GitHub sign-in failed"
                challenge = null
            } finally {
                finishingSignIn = false
            }
        }

        AlertDialog(
            onDismissRequest = { dialogOpen = false; challenge = null },
            title = { Text("GitHub account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        challenge != null -> {
                            Text("Open GitHub and enter this code to authorize NikGapps:")
                            SelectionContainer { Text(challenge!!.userCode) }
                            Text(if (finishingSignIn) "Finishing GitHub sign-in…" else "Waiting for authorization…")
                            CircularProgressIndicator()
                        }
                        loading -> CircularProgressIndicator()
                        GithubPrefs.token.isNotBlank() -> Text(
                            "${GithubPrefs.username.ifBlank { "A token" }} is connected. " +
                                "NikGapps can use this account for GitHub requests."
                        )
                        clientId.isBlank() -> Text("This build needs a NikGapps GitHub OAuth client ID to enable account sign-in. You can still add a personal access token below.")
                        else -> Text("Use your GitHub account to authorize NikGapps. A code will appear here; no password is entered in the app.")
                    }
                    error?.let { Text(it) }
                }
            },
            confirmButton = {
                when {
                    challenge != null -> TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(challenge!!.verificationUri)))
                    }) { Text("Open GitHub") }
                    !loading && clientId.isNotBlank() -> TextButton(onClick = {
                        error = null
                        loading = true
                        scope.launch {
                            try {
                                challenge = GitHubDeviceAuth.start(clientId)
                            } catch (failure: Exception) {
                                error = failure.message ?: "Could not start GitHub sign-in"
                            } finally {
                                loading = false
                            }
                        }
                    }) { Text(if (GithubPrefs.token.isBlank()) "Sign in" else "Switch account") }
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false; challenge = null }) { Text("Close") }
                if (GithubPrefs.token.isNotBlank()) {
                    TextButton(onClick = {
                        GithubPrefs.token = ""
                        GithubPrefs.username = ""
                        GithubPrefs.avatarUrl = ""
                        dialogOpen = false
                        challenge = null
                    }) { Text("Sign out") }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

package com.nikgapps.app.presentation.ui.component.containers

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.CircularProgressIndicator
import com.nikgapps.app.presentation.ui.component.GlassButton as Button
import com.nikgapps.app.presentation.ui.component.GlassTonalButton as FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.nikgapps.R
import com.nikgapps.BuildConfig
import com.nikgapps.app.data.GithubPrefs
import com.nikgapps.app.presentation.ui.component.items.PreferenceItem
import com.nikgapps.app.presentation.ui.component.dialogs.AccountDialog
import com.nikgapps.app.utils.network.GitHubDeviceAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    var startJob by remember { mutableStateOf<Job?>(null) }
    var requestGeneration by remember { mutableIntStateOf(0) }
    val clientId = BuildConfig.GITHUB_CLIENT_ID

    fun dismissDialog() {
        requestGeneration++
        startJob?.cancel()
        startJob = null
        loading = false
        challenge = null
        dialogOpen = false
    }

    fun openVerificationPage(active: GitHubDeviceAuth.Challenge) {
        try {
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("GitHub sign-in code", active.userCode))
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(active.verificationUri)))
        } catch (failure: Exception) {
            error = "Could not open GitHub. Copy the code above and open ${active.verificationUri}."
        }
    }

    fun beginSignIn() {
        error = null
        dialogOpen = true
        if (clientId.isBlank() || loading || challenge != null) return
        loading = true
        val generation = ++requestGeneration
        startJob = scope.launch {
            try {
                val nextChallenge = GitHubDeviceAuth.start(clientId)
                if (dialogOpen && generation == requestGeneration) {
                    challenge = nextChallenge
                    openVerificationPage(nextChallenge)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (dialogOpen && generation == requestGeneration) {
                    error = failure.message ?: "Could not start GitHub sign-in"
                }
            } finally {
                if (generation == requestGeneration) {
                    loading = false
                    startJob = null
                }
            }
        }
    }

    if (asSignInButton) {
        Button(
            onClick = ::beginSignIn,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(painterResource(R.drawable.ic_github_mark), contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(if (GithubPrefs.lastUsername.isBlank()) "Continue with GitHub" else "Reconnect with GitHub")
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
        onClick = { error = null; dialogOpen = true }
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
                GithubPrefs.lastUsername = profile.login
                GithubPrefs.avatarUrl = profile.avatarUrl
                GithubPrefs.guestMode = false
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

        val colors = MaterialTheme.colorScheme
        val canContinue = challenge != null || (!loading && clientId.isNotBlank())
        AccountDialog(
            onDismiss = ::dismissDialog,
            title = "GitHub account",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        challenge != null -> {
                            Text("Open GitHub and enter this code to authorize NikGapps:",
                                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            com.nikgapps.app.presentation.ui.component.GlassSurface(shape = RoundedCornerShape(16.dp), color = colors.surfaceContainerHighest) {
                                SelectionContainer {
                                    Text(challenge!!.userCode,
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        style = MaterialTheme.typography.headlineSmall,
                                        textAlign = TextAlign.Center)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Text(if (finishingSignIn) "Finishing GitHub sign-in…" else "Waiting for authorization…",
                                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            }
                        }
                        loading -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Text("Preparing GitHub sign-in…", style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant)
                        }
                        GithubPrefs.token.isNotBlank() -> Text(
                            "${GithubPrefs.username.ifBlank { "A token" }} is connected. " +
                                "NikGapps can use this account for GitHub requests.",
                            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant
                        )
                        clientId.isBlank() -> Text("This build needs a NikGapps GitHub OAuth client ID to enable account sign-in. You can still add a personal access token below.",
                            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        else -> Text("Use your GitHub account to authorize NikGapps. A code will appear here; no password is entered in the app.",
                            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error) }
                }
            },
            actions = {
                if (canContinue) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = ::dismissDialog,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp)) {
                            Text("Close")
                        }
                        Button(onClick = {
                            val active = challenge
                            if (active != null) openVerificationPage(active) else beginSignIn()
                        }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp)) {
                            Text(when {
                                challenge != null -> "Open GitHub"
                                GithubPrefs.token.isBlank() -> "Sign in"
                                else -> "Switch account"
                            }, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    Button(onClick = ::dismissDialog, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(16.dp)) { Text("Close") }
                }
                if (GithubPrefs.token.isNotBlank()) {
                    FilledTonalButton(onClick = {
                        if (GithubPrefs.username.isNotBlank()) GithubPrefs.lastUsername = GithubPrefs.username
                        dismissDialog()
                        GithubPrefs.token = ""
                        GithubPrefs.username = ""
                        GithubPrefs.avatarUrl = ""
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp)) {
                        Text("Sign out")
                    }
                }
            }
        )
    }
}

package com.nikgapps.app.presentation.ui.screen

import com.nikgapps.app.presentation.ui.component.AppIcons
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Settings
import com.nikgapps.app.presentation.ui.component.GlassButton as Button
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledTonalIconButton
import com.nikgapps.app.presentation.ui.component.GlassTonalButton as FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.nikgapps.app.data.GITHUB_SIGN_IN_EXPLANATION
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.nikgapps.R
import com.nikgapps.app.data.GithubPrefs
import com.nikgapps.app.data.DisplayPrefs
import com.nikgapps.app.presentation.ui.component.containers.GitHubAccountPreference
import com.nikgapps.app.presentation.ui.component.containers.GitHubTokenTopSheet
import com.nikgapps.app.presentation.ui.component.dialogs.AccountDialog

@Composable
fun AccountGate(onOpenSettings: () -> Unit, onContinueAsGuest: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val allowGuest = DisplayPrefs.guestAccessEnabled
    var showWhyGitHub by rememberSaveable { mutableStateOf(false) }
    var showTokenEditor by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxSize().background(if (DisplayPrefs.useLiquidGlass)
            androidx.compose.ui.graphics.Color.Transparent else colors.background).safeDrawingPadding().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        FilledTonalIconButton(
            onClick = onOpenSettings,
            modifier = Modifier.align(Alignment.TopEnd).size(48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
        }
        Column(
            modifier = Modifier.widthIn(max = 440.dp).padding(vertical = 56.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.nikgapps_logo),
                contentDescription = "NikGapps",
                modifier = Modifier.size(88.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(if (GithubPrefs.lastUsername.isBlank()) "Welcome to NikGapps"
                 else "Welcome back, ${GithubPrefs.lastUsername}", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                color = colors.onBackground)
            Spacer(Modifier.height(8.dp))
            Text(if (allowGuest) "Sign in with GitHub or continue as a guest." else "Sign in with GitHub to get started.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            com.nikgapps.app.presentation.ui.component.GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceContainerHigh,
                border = BorderStroke(1.dp, colors.outlineVariant)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Sign in with GitHub", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(if (GithubPrefs.lastUsername.isBlank())
                        "A one-time code will be copied and GitHub will open in your browser."
                    else "A one-time code will be copied. Check that GitHub is using ${GithubPrefs.lastUsername}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = colors.outlineVariant)
                    Spacer(Modifier.height(16.dp))
                    GitHubAccountPreference(asSignInButton = true)
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = { showWhyGitHub = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(16.dp)) {
                        Icon(AppIcons.Info, null, Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Why GitHub?")
                    }

                }
            }
            Spacer(Modifier.height(24.dp))
            if (allowGuest) {
                FilledTonalButton(onClick = onContinueAsGuest, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(16.dp)) { Text("Continue as guest") }
                Spacer(Modifier.height(8.dp))
                Text("Create projects and build ZIPs with the normal build limit. Sign in later for Elite benefits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
            }
            FilledTonalButton(onClick = { showTokenEditor = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Outlined.Key, null, Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text("Use a GitHub token")
            }
            Spacer(Modifier.height(12.dp))
            Text("Connect directly with a personal access token.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
    if (showWhyGitHub) {
        AccountDialog(
            onDismiss = { showWhyGitHub = false },
            title = "Why GitHub?",
            content = {
                Text(GITHUB_SIGN_IN_EXPLANATION,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant)
            },
            actions = {
                Button(onClick = { showWhyGitHub = false },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(16.dp)) { Text("Got it") }
            }
        )
    }
    if (showTokenEditor) {
        GitHubTokenTopSheet(
            onDismiss = { showTokenEditor = false },
            onSaved = { showTokenEditor = false }
        )
    }

}

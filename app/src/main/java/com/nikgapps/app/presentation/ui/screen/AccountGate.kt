package com.nikgapps.app.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.nikgapps.R
import com.nikgapps.app.presentation.ui.component.containers.GitHubAccountPreference
import com.nikgapps.app.presentation.ui.component.containers.GitHubTokenEntry

@Composable
fun AccountGate() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxSize().background(colors.background).imePadding().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 440.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.nikgapps_logo),
                contentDescription = "NikGapps",
                modifier = Modifier.size(88.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text("Welcome to NikGapps", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                color = colors.onBackground)
            Spacer(Modifier.height(8.dp))
            Text("Sign in with GitHub to get started.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceContainerLow,
                tonalElevation = 1.dp
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Sign in with a token", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Enter your GitHub personal access token to continue.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    GitHubTokenEntry(showExplanation = false)
                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f))
                        Text("  or  ", color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium)
                        HorizontalDivider(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Sign in with GitHub", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Authorize in your browser with a one-time code.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    GitHubAccountPreference(asSignInButton = true)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Your GitHub password is never entered in NikGapps.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}

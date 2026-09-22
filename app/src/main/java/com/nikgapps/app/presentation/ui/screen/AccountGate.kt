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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.nikgapps.R
import com.nikgapps.app.data.GithubPrefs
import com.nikgapps.app.presentation.ui.component.containers.GitHubAccountPreference

@Composable
fun AccountGate() {
    var guestUsername by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxSize().background(colors.background).padding(24.dp),
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
            Text("Sign in with GitHub or continue as a guest.",
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
                    Text("Sign in", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Authorize securely in your browser with a one-time code.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                    GitHubAccountPreference(asSignInButton = true)
                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f))
                        Text("  or  ", color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium)
                        HorizontalDivider(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Continue as a guest", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Choose a username to use the app without GitHub.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = guestUsername,
                        onValueChange = { guestUsername = it },
                        label = { Text("Username") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { GithubPrefs.guestUsername = guestUsername.trim() },
                        enabled = guestUsername.trim().isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Continue as guest") }
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

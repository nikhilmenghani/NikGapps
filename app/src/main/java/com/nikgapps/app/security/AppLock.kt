package com.nikgapps.app.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun canUseAppLock(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    return context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
}

@Composable
fun AppLock(enabled: Boolean, activity: Activity, content: @Composable () -> Unit) {
    var unlocked by remember(enabled) { mutableStateOf(!enabled) }
    var request by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    if (unlocked) { content(); return }

    LaunchedEffect(request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            authenticateForAppLock(activity, { unlocked = true }, { error = it })
        } else error = "App lock is unavailable on this Android version"
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Fingerprint, contentDescription = null)
        Text("Unlock NikGapps", style = MaterialTheme.typography.headlineSmall)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { error = null; request++ }) { Text("Authenticate") }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
fun authenticateForAppLock(
    activity: Activity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    title: String = "Unlock NikGapps",
    subtitle: String = "Authenticate to continue"
) {
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) = onSuccess()
        override fun onAuthenticationError(code: Int, message: CharSequence?) =
            onError(message?.toString().orEmpty().ifBlank { "Authentication cancelled" })
    }
    runCatching {
        val builder = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                builder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                @Suppress("DEPRECATION") builder.setDeviceCredentialAllowed(true)
            else -> builder.setNegativeButton("Cancel", activity.mainExecutor) { _, _ ->
                onError("Authentication cancelled")
            }
        }
        builder.build().authenticate(CancellationSignal(), activity.mainExecutor, callback)
    }.onFailure { failure ->
        onError(failure.message ?: "Authentication is unavailable")
    }
}

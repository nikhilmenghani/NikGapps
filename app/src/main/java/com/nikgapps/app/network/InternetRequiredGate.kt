package com.nikgapps.app.network

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nikgapps.app.registry.CatalogRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private enum class InternetState { CHECKING, ONLINE, OFFLINE }

val LocalInternetAvailable = staticCompositionLocalOf { false }

private val reachabilityClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.SECONDS)
    .callTimeout(7, TimeUnit.SECONDS)
    .build()

@Composable
fun InternetRequiredGate(
    required: Boolean,
    allowOffline: Boolean,
    onOpenAppSettings: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val connectivityManager = remember {
        context.getSystemService(ConnectivityManager::class.java)
    }
    val scope = rememberCoroutineScope()
    var internetState by remember { mutableStateOf(InternetState.CHECKING) }
    var checkJob by remember { mutableStateOf<Job?>(null) }

    fun checkInternet(showChecking: Boolean, confirmationDelayMs: Long = 0) {
        checkJob?.cancel()
        checkJob = scope.launch {
            if (showChecking) internetState = InternetState.CHECKING
            if (confirmationDelayMs > 0) delay(confirmationDelayMs)
            internetState = if (
                connectivityManager.hasValidatedInternet() && canReachPackageCatalog()
            ) InternetState.ONLINE else InternetState.OFFLINE
        }
    }

    LaunchedEffect(Unit) { checkInternet(showChecking = true) }

    DisposableEffect(connectivityManager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (internetState != InternetState.ONLINE) checkInternet(showChecking = false)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    if (internetState != InternetState.ONLINE) checkInternet(showChecking = false)
                } else {
                    checkInternet(showChecking = false, confirmationDelayMs = 1_500)
                }
            }

            override fun onLost(network: Network) {
                checkInternet(showChecking = false, confirmationDelayMs = 1_500)
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        onDispose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    val showGate = required && !allowOffline && internetState != InternetState.ONLINE
    BackHandler(enabled = showGate) {
        // Keep the current workflow and navigation destination intact while offline.
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Always keep the application composed. Removing it here would recreate the NavHost and
        // discard in-progress screen state when connectivity returns.
        CompositionLocalProvider(
            LocalInternetAvailable provides (internetState == InternetState.ONLINE)
        ) { content() }

        if (showGate) {
            OfflineScreen(
                checking = internetState == InternetState.CHECKING,
                onRetry = { checkInternet(showChecking = true) },
                onOpenSettings = {
                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                },
                onOpenAppSettings = onOpenAppSettings
            )
        }
    }
}

private fun ConnectivityManager.hasValidatedInternet(): Boolean {
    val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

private suspend fun canReachPackageCatalog(): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val request = Request.Builder()
            .url(CatalogRepository.CATALOG_URL)
            .header("Range", "bytes=0-0")
            .build()
        reachabilityClient.newCall(request).execute().use { it.isSuccessful }
    }.getOrDefault(false)
}

@Composable
private fun OfflineScreen(
    checking: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            ),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(88.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(44.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.CloudOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }
                    Text(
                        if (checking) "Checking connection..." else "You're offline",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        if (checking) "Connecting to the NikGapps package catalog."
                        else "NikGapps needs an internet connection to download GApps packages before building them.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = onRetry,
                        enabled = !checking,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Try again") }
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Network settings") }
                    OutlinedButton(
                        onClick = onOpenAppSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("NikGapps settings") }
                }
            }
        }
    }
}

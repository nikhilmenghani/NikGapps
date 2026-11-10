package com.nikgapps.app.presentation.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.runtime.*
import com.nikgapps.app.presentation.ui.component.items.PreferenceItem
import com.nikgapps.app.presentation.ui.component.items.PreferenceSubtitle
import com.nikgapps.app.registry.RequestLimitServer
import com.nikgapps.app.registry.ServerRequestLimits
import com.nikgapps.app.registry.registryRetryTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun ServerRequestLimitsPreference() {
    val observations by ServerRequestLimits.observations.collectAsState()
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf<RequestLimitServer?>(null) }
    var errors by remember { mutableStateOf<Map<RequestLimitServer, String>>(emptyMap()) }
    PreferenceSubtitle(text = "Server request limits")
    for (server in RequestLimitServer.entries) {
        val observation = observations[server]
        val details = buildList {
            add(if (server == RequestLimitServer.GITLAB) "Anonymous requests from this network" else "Signed-in GitHub account")
            observation?.let {
                add("Last observed: ${it.remaining ?: "unknown"} / ${it.limit ?: "unknown"} remaining · HTTP ${it.httpStatus}")
                it.resetAtMillis?.let { reset -> add("Window reset: ${registryRetryTime(reset)}") }
                it.retryAtMillis?.let { retry -> add("Retry after: ${registryRetryTime(retry)}") }
                add("Checked: ${registryRetryTime(it.observedAtMillis)}")
                it.bucket?.let { bucket -> add("Limit: $bucket") }
            }
            errors[server]?.let(::add)
            add(if (checking == server) "Checking…" else "Tap to check once. Other server limits may apply.")
        }.joinToString("\n")
        PreferenceItem(label = "${server.title} requests", supportingText = details,
            icon = Icons.Outlined.Wifi, onClick = {
                if (checking == null) scope.launch {
                    checking = server
                    errors = errors - server
                    try { ServerRequestLimits.check(server) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { errors = errors + (server to "Could not check. Verify your connection and GitHub sign-in.") }
                    finally { checking = null }
                }
            })
    }
}

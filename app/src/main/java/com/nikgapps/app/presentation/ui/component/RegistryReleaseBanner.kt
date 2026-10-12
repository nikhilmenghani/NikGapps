package com.nikgapps.app.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nikgapps.app.data.ReleaseNoticeRepository
import com.nikgapps.app.registry.*
import kotlinx.coroutines.CancellationException

@Composable
fun RegistryReleaseBanner(online: Boolean) {
    val context = LocalContext.current
    val preferences = remember { ReleaseNoticeRepository(context) }
    var release by remember { mutableStateOf<ReleaseSummary?>(null) }
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(online) {
        if (online) try {
            val metadata = CatalogRepository(context.cacheDir).load(forceRefresh = true)
            val latest = metadata.releaseIndex?.latest?.values?.flatMap { it.values.flatMap { architectures -> architectures.values } }.orEmpty().toSet()
            release = metadata.releaseIndex?.releases?.filter { it.id in latest && it.channel == "stable" }
                ?.maxByOrNull { it.createdAt }?.takeUnless { preferences.dismissed(it.id) }
        } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { }
    }
    release?.let { published ->
        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A new NikGapps release is available", style = MaterialTheme.typography.titleMedium)
                Text("Updated packages and fixes are available for Android ${published.androidVersion}. Build an updated ZIP for your compatible projects.")
                TextButton(onClick = { confirm = true }) { Text("Dismiss") }
            }
        }
        if (confirm) AlertDialog(onDismissRequest = { confirm = false },
            title = { Text("Dismiss this release announcement?") },
            text = { Text("Project update indicators will remain visible. You'll see another announcement when a newer release is available.") },
            confirmButton = { TextButton(onClick = { preferences.dismiss(published.id); release = null; confirm = false }) { Text("Dismiss") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep") } })
    }
}

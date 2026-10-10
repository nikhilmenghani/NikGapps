package com.nikgapps.app.presentation.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nikgapps.R
import com.nikgapps.app.data.BuildDatePrefs
import com.nikgapps.app.data.GithubPrefs
import com.nikgapps.app.presentation.ui.component.items.PreferenceItem
import com.nikgapps.app.presentation.ui.component.items.CheckableItem
import com.nikgapps.app.presentation.ui.component.dialogs.BottomSheetDialog
import com.nikgapps.app.utils.network.EliteMembershipRepository
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EliteBuildOptions() {
    var elite by remember(GithubPrefs.username) { mutableStateOf(false) }
    var showDates by remember { mutableStateOf(false) }
    var showFeatures by remember { mutableStateOf(false) }
    LaunchedEffect(GithubPrefs.username) {
        elite = try { EliteMembershipRepository.isElite(GithubPrefs.username) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { false }
    }
    PreferenceItem(label = "ZIP filename date",
        supportingText = if (elite) {
            if (BuildDatePrefs.useCurrentDate) "Current date" else "Release date"
        } else "Elite required · Release date",
        icon = Icons.Outlined.CalendarMonth, enabled = elite, eliteOption = true,
        onClick = { showDates = true })
    PreferenceItem(label = "Elite features", supportingText = "See what NikGapps Elites get",
        icon = Icons.Outlined.Star, onClick = { showFeatures = true })
    if (showDates && elite) BottomSheetDialog(onDismissRequest = { showDates = false }) {
        Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
            Text("ZIP filename date", style = MaterialTheme.typography.titleLarge)
            Text("Choose the date in your ZIP filename. Package versions and release metadata stay unchanged.",
                Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium)
            CheckableItem(text = "Release date", isChecked = !BuildDatePrefs.useCurrentDate, icon = null,
                onClick = { BuildDatePrefs.useCurrentDate = false; showDates = false })
            CheckableItem(text = "Current date", isChecked = BuildDatePrefs.useCurrentDate, icon = null,
                onClick = { BuildDatePrefs.useCurrentDate = true; showDates = false })
        }
    }
    if (showFeatures) EliteFeaturesDialog(elite) { showFeatures = false }
}

@Composable
private fun EliteFeaturesDialog(elite: Boolean, onDismiss: () -> Unit) {
    val features = listOf(
        "Elite badge" to "Your dashboard shows an Elite badge when your GitHub account is eligible.",
        "Extra builds" to "Reset your build window to add six builds to your remaining allowance. Available once per six-hour window.",
        "Compression control" to "Choose compression levels 1–9 to balance ZIP size and build time.",
        "ZIP filename date" to "Choose the package release date or today's device-local date for your ZIP filename.",
        "Longer project names" to "Name projects with up to 25 characters, instead of the standard 20."
    )
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.94f).heightIn(max = (LocalConfiguration.current.screenHeightDp * .85f).dp),
            shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column {
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.nikgapps_logo), null, Modifier.size(48.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Elite features", style = MaterialTheme.typography.titleLarge)
                        Text(if (elite) "NikGapps · Elite enabled" else "NikGapps · For eligible GitHub accounts",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                LazyColumn(Modifier.weight(1f, fill = false), contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    features.forEach { (name, description) -> item {
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(name, style = MaterialTheme.typography.titleMedium)
                                Text(description, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } }
                }
                HorizontalDivider()
                FilledTonalButton(onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(16.dp)) { Text("Close") }
            }
        }
    }
}

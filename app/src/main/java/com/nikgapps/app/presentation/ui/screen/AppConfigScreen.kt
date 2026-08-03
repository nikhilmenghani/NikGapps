package com.nikgapps.app.presentation.ui.screen

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nikgapps.app.data.*
import com.nikgapps.app.registry.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(projectId: String, packageId: String, navController: NavHostController) {
    val context = LocalActivity.current ?: return
    val repository = remember { BuildProjectRepository(context) }
    var project by remember(projectId) { mutableStateOf(repository.getProjects().firstOrNull { it.id == projectId }) }
    var metadata by remember { mutableStateOf<RegistryMetadata?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val catalogRepository = remember { CatalogRepository(context.cacheDir) }

    LaunchedEffect(Unit) {
        try { metadata = catalogRepository.load() }
        catch (e: Exception) { error = e.message ?: "Unable to load package information" }
    }
    val current = project
    val pkg = metadata?.catalog?.packages?.firstOrNull { it.id == packageId }
    val memberAppSets = metadata?.let { loaded -> loaded.appSets.appSets.filter { set ->
        loaded.catalog.publicPackages(set).any { it.id == packageId }
    } }.orEmpty()
    val channelName = current?.channelOverrides?.get(packageId) ?: current?.defaultChannel ?: "stable"
    val channel = ReleaseChannel.entries.firstOrNull { it.wireName == channelName } ?: ReleaseChannel.STABLE
    val version = pkg?.channels?.get(channel.wireName)?.let { pkg.versions[it] } ?: pkg?.versions?.values?.firstOrNull()
    val installedInfo = remember(pkg) { pkg?.versions?.values?.asSequence()?.mapNotNull { candidate ->
        candidate.packageName?.let { name -> context.packageInfoOrNull(name) }
    }?.firstOrNull() }
    val installedVersionCode = installedInfo?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else @Suppress("DEPRECATION") it.versionCode.toLong()
    }
    val source = current?.appSources?.get(packageId)?.source ?: AppSource.GITLAB
    val owner = memberAppSets.firstOrNull { it.id == current?.selectedPackageAppSets?.get(packageId) } ?: memberAppSets.firstOrNull()

    fun save(value: BuildProject) { repository.updateProject(value); project = value }

    Scaffold(topBar = { TopAppBar(title = { Text(pkg?.name ?: "Package") }, navigationIcon = {
        IconButton(onClick = navController::navigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
    }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(48.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Android, null, Modifier.size(24.dp)) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pkg?.name ?: "Loading…", style = MaterialTheme.typography.titleLarge,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(version?.packageName ?: packageId, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            if (metadata == null && error == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (pkg != null && current != null) {
                item {
                    SectionTitle("Package information", "The version changes with the selected GitLab channel.")
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        AnimatedContent(version, label = "package-version") { shown ->
                            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                InfoRow("Package", shown?.packageName ?: "Not specified")
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                InfoRow("Version", shown?.versionName ?: "Unavailable")
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                InfoRow("Version code", shown?.versionCode?.toString() ?: "Unavailable")
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                InfoRow("Android", listOfNotNull(shown?.android?.minApi?.let { "API $it+" }, shown?.android?.maxApi?.let { "up to $it" }).joinToString(" · ").ifBlank { "Any supported API" })
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                InfoRow("Architecture", shown?.architectures?.joinToString().orEmpty().ifBlank { "Universal" })
                            }
                        }
                    }
                }
                item {
                    SectionTitle("Package source", "Choose the complete package and version used in the ZIP.")
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FullSourceCard("NikGapps GitLab", version?.versionName, Icons.Default.CloudDownload,
                            selected = source == AppSource.GITLAB, enabled = true,
                            supporting = if (installedVersionCode != null && version != null && installedVersionCode > version.versionCode)
                                "A newer version is installed on this device" else "Catalog release · ${channel.wireName.replaceFirstChar { it.uppercase() }}") {
                            save(current.copy(appSources = current.appSources + (packageId to AppSourceConfig(AppSource.GITLAB))))
                        }
                        FullSourceCard("This device", installedInfo?.versionName, Icons.Default.PhoneAndroid,
                            selected = source == AppSource.DEVICE, enabled = installedInfo != null,
                            supporting = if (installedInfo == null) "Package is not installed" else "Use the installed APK") {
                            save(current.copy(appSources = current.appSources + (packageId to AppSourceConfig(AppSource.DEVICE))))
                        }
                    }
                }
                item {
                    SectionTitle("Release channel", "Selecting a channel immediately updates the GitLab version above.")
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        FlowRow(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReleaseChannel.entries.forEach { option ->
                                val available = option.wireName in pkg.channels
                                FilterChip(selected = channel == option, enabled = available,
                                    onClick = { save(current.copy(channelOverrides = current.channelOverrides + (packageId to option.wireName))) },
                                    label = { Text(option.wireName.replaceFirstChar { it.uppercase() }) },
                                    leadingIcon = if (channel == option) {{ Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }} else null)
                            }
                        }
                    }
                }
                item {
                    SectionTitle("AppSet", "Choose where this package should be placed in the flashable ZIP.")
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        FlowRow(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            memberAppSets.forEach { set -> FilterChip(selected = owner?.id == set.id,
                                onClick = { save(current.copy(selectedAppSetId = set.id,
                                    selectedPackageAppSets = current.selectedPackageAppSets + (packageId to set.id))) },
                                label = { Text(set.name) }, leadingIcon = if (owner?.id == set.id) {{ Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }} else null) }
                        }
                    }
                }
            }
        }
    }
}

private fun android.content.Context.packageInfoOrNull(name: String): PackageInfo? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        packageManager.getPackageInfo(name, PackageManager.PackageInfoFlags.of(0))
    else @Suppress("DEPRECATION") packageManager.getPackageInfo(name, 0)
}.getOrNull()

@Composable private fun SectionTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(2.dp))
    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable private fun FullSourceCard(title: String, version: String?, icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean, enabled: Boolean, supporting: String, onClick: () -> Unit) {
    Card(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp), border = BorderStroke(2.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(22.dp)); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(version?.let { "Version $it" } ?: "Version unavailable", style = MaterialTheme.typography.bodySmall)
                Text(supporting, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(selected) { Icon(Icons.Default.CheckCircle, "Selected source", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

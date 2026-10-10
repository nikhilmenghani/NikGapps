package com.nikgapps.app.presentation.ui.screen

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import android.text.format.DateFormat
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import kotlin.math.roundToInt
import androidx.navigation.NavHostController
import com.nikgapps.app.data.*
import com.nikgapps.app.presentation.navigation.appConfigRoute
import com.nikgapps.app.presentation.navigation.buildZipRoute
import com.nikgapps.app.presentation.navigation.projectRoute
import com.nikgapps.app.registry.*
import com.nikgapps.app.utils.ZipBuildProgress
import com.nikgapps.app.utils.AppDiagnostics
import com.nikgapps.app.utils.network.EliteMembershipRepository
import com.nikgapps.app.network.LocalInternetAvailable
import com.nikgapps.app.utils.network.GitHubBuildAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

private data class RegistryDeviceStatus(
    val installed: Boolean,
    val versionName: String? = null,
    val versionCode: Long? = null
)

private enum class PackageSort(val label: String) {
    NAME("Name"),
    INSTALLED("Install status"),
    SELECTED("Selection status")
}

private enum class SelectionFilter(val label: String) {
    BOTH("Both"),
    SELECTED("Selected"),
    UNSELECTED("Unselected")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(projectId: String, autoBuild: Boolean = false, navController: NavHostController) {
    val context = LocalActivity.current ?: return
    val isOnline = LocalInternetAvailable.current
    val repository = remember { BuildProjectRepository(context) }
    var project by remember(projectId) { mutableStateOf(repository.getProjects().firstOrNull { it.id == projectId }) }
    var metadata by remember { mutableStateOf<RegistryMetadata?>(null) }
    var metadataRefreshes by rememberSaveable(projectId) { mutableIntStateOf(0) }
    var consumedMetadataRefreshes by rememberSaveable(projectId) { mutableIntStateOf(0) }
    var metadataLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<ZipBuildProgress?>(null) }
    var result by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var packageSort by rememberSaveable(projectId) { mutableStateOf(PackageSort.NAME) }
    var sortDescending by rememberSaveable(projectId) { mutableStateOf(false) }
    var installedOnly by rememberSaveable(projectId) { mutableStateOf(false) }
    var selectionFilter by rememberSaveable(projectId) { mutableStateOf(SelectionFilter.BOTH) }
    var sortExpanded by rememberSaveable(projectId) { mutableStateOf(false) }
    var filterExpanded by rememberSaveable(projectId) { mutableStateOf(false) }
    var notificationsExpanded by rememberSaveable(projectId) { mutableStateOf(false) }
    var summaryExpanded by rememberSaveable(projectId) { mutableStateOf(false) }
    var appSetView by rememberSaveable(projectId) { mutableStateOf(true) }
    var expandedAppSets by rememberSaveable(projectId) { mutableStateOf(emptyList<String>()) }
    var searchInput by rememberSaveable(projectId, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val searchQuery = searchInput.text
    var searchVisible by rememberSaveable(projectId) { mutableStateOf(false) }
    var quotaClock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var eliteMember by remember { mutableStateOf(false) }
    var resettingEliteQuota by remember { mutableStateOf(false) }
    var confirmEliteResetRemaining by remember { mutableStateOf<Int?>(null) }
    var autoBuildConsumed by rememberSaveable(projectId, autoBuild) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val catalogRepository = remember { CatalogRepository(context.cacheDir) }
    var pendingConfig by rememberSaveable(projectId) { mutableStateOf<String?>(null) }
    var exportingConfig by remember { mutableStateOf(false) }
    var importingConfig by remember { mutableStateOf(false) }
    // A text/plain MIME type makes some document providers append .txt to .config.
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val content = pendingConfig
        pendingConfig = null
        if (uri != null && content != null) scope.launch {
            exportingConfig = true
            try {
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri, "wt")
                        ?: error("Cannot open the selected file")
                    val lfContent = content.replace("\r\n", "\n").replace('\r', '\n')
                    output.use { it.write(lfContent.toByteArray(Charsets.UTF_8)) }
                }
                Toast.makeText(context, "Config exported", Toast.LENGTH_SHORT).show()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Toast.makeText(context, error.message ?: "Unable to export config", Toast.LENGTH_LONG).show()
            } finally { exportingConfig = false }
        }
    }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val appsListState = rememberLazyListState()

    LaunchedEffect(searchVisible) {
        if (searchVisible) {
            searchInput = searchInput.copy(selection = TextRange(searchInput.text.length))
            searchFocusRequester.requestFocus()
            keyboard?.show()
        }
    }
    LaunchedEffect(notificationsExpanded) {
        while (notificationsExpanded) {
            quotaClock = System.currentTimeMillis()
            delay(30_000L)
        }
    }
    LaunchedEffect(notificationsExpanded, GithubPrefs.username, isOnline) {
        eliteMember = false
        if (notificationsExpanded && isOnline && GithubPrefs.username.isNotBlank()) {
            try {
                eliteMember = EliteMembershipRepository.isElite(GithubPrefs.username)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Only offer a reset after a successful membership lookup.
            }
        }
    }

    val current = project
    LaunchedEffect(current?.androidVersion, current?.architecture, current?.defaultChannel, isOnline, metadataRefreshes) {
        if (current == null) return@LaunchedEffect
        if (!isOnline) {
            metadata = null
            loadError = "Internet connection is required to list the supported packages"
            return@LaunchedEffect
        }
        loadError = null
        metadataLoading = true
        val forceRefresh = metadataRefreshes > consumedMetadataRefreshes
        try { metadata = catalogRepository.load(catalogAndroidVersion(current.androidVersion.displayName),
            current.defaultChannel, current.architecture.value, forceRefresh = forceRefresh) }
        catch (e: Exception) { loadError = e.message ?: "Unable to load the NikGapps catalog" }
        finally {
            if (forceRefresh) consumedMetadataRefreshes = metadataRefreshes
            metadataLoading = false
        }
    }
    if (current == null) { Text("Project not found", Modifier.padding(24.dp)); return }
    val registry = metadata
    val selectedAppSet = registry?.appSets?.appSets?.firstOrNull { it.id == current.selectedAppSetId }
        ?: registry?.appSets?.appSets?.firstOrNull()
    val packageAppSets = registry?.let { loaded ->
        loaded.appSets.appSets.flatMap { appSet ->
            loaded.catalog.publicPackages(appSet).map { pkg -> pkg.id to appSet }
        }.groupBy({ it.first }, { it.second })
    }.orEmpty()
    val displayedPackages = registry?.catalog?.packages?.filter { it.id in packageAppSets }.orEmpty()
    val unavailable = registry?.let { unavailableProjectPackages(current, it) }.orEmpty()

    LaunchedEffect(registry, selectedAppSet?.id) {
        if (registry != null && selectedAppSet != null && unavailable.isEmpty()) {
            val packageOwners = current.selectedPackageAppSets.toMutableMap()
            current.selectedAppIds.forEach { id ->
                val validOwners = packageAppSets[id].orEmpty()
                if (packageOwners[id] !in validOwners.map { it.id }) {
                    packageOwners[id] = validOwners.firstOrNull()?.id ?: selectedAppSet.id
                }
            }
            val normalized = current.copy(selectedAppSetId = selectedAppSet.id, selectedPackageAppSets = packageOwners)
            if (normalized != current) { repository.updateProject(normalized); project = normalized }
        }
    }
    val catalogPackages = registry?.catalog?.packages.orEmpty()
    val deviceStatuses by produceState<Map<String, RegistryDeviceStatus>>(emptyMap(), catalogPackages) {
        value = withContext(Dispatchers.IO) { catalogPackages.associate { pkg ->
            val info = pkg.versions.values.asSequence().mapNotNull { version ->
                version.packageName?.let { packageName -> runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context.packageManager.getPackageInfo(
                        packageName, PackageManager.PackageInfoFlags.of(0))
                    else @Suppress("DEPRECATION") context.packageManager.getPackageInfo(packageName, 0)
                }.getOrNull() }
            }.firstOrNull()
            pkg.id to if (info == null) RegistryDeviceStatus(false) else RegistryDeviceStatus(
                installed = true,
                versionName = info.versionName,
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
                    else @Suppress("DEPRECATION") info.versionCode.toLong()
            )
        } }
    }
    val sortedPackages = remember(displayedPackages, packageAppSets, deviceStatuses, packageSort, sortDescending, installedOnly, appSetView,
        selectionFilter, current.selectedAppIds, searchQuery) {
        val query = searchQuery.trim().lowercase()
        val filtered = displayedPackages.filter { pkg ->
            (query.isEmpty() || pkg.name.lowercase().contains(query) || pkg.id.lowercase().contains(query) ||
                packageAppSets[pkg.id].orEmpty().any { it.name.lowercase().contains(query) } ||
                pkg.versions.values.any { it.packageName?.lowercase()?.contains(query) == true }) &&
            (!installedOnly || deviceStatuses[pkg.id]?.installed == true) && (appSetView || when (selectionFilter) {
                SelectionFilter.BOTH -> true
                SelectionFilter.SELECTED -> pkg.id in current.selectedAppIds
                SelectionFilter.UNSELECTED -> pkg.id !in current.selectedAppIds
            })
        }
        when (packageSort) {
            PackageSort.NAME -> filtered.sortedBy { it.name.lowercase() }.let { if (sortDescending) it.reversed() else it }
            PackageSort.INSTALLED -> (if (sortDescending)
                compareByDescending<CatalogPackage> { deviceStatuses[it.id]?.installed == true }
                else compareBy<CatalogPackage> { deviceStatuses[it.id]?.installed == true })
                .thenBy { it.name.lowercase() }.let(filtered::sortedWith)
            PackageSort.SELECTED -> (if (sortDescending)
                compareByDescending<CatalogPackage> { it.id in current.selectedAppIds }
                else compareBy<CatalogPackage> { it.id in current.selectedAppIds })
                .thenBy { it.name.lowercase() }.let(filtered::sortedWith)
        }
    }

    fun save(value: BuildProject) { repository.updateProject(value); project = value }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            importingConfig = true
            try {
                val updated = withContext(Dispatchers.IO) {
                    val loaded = catalogRepository.load(catalogAndroidVersion(current.androidVersion.displayName),
                        current.defaultChannel, current.architecture.value, forceRefresh = true)
                    require(!loaded.fromCache) { "Unable to verify the latest config version. Please try again online." }
                    val template = AndroidBuilderAssetSource(context, loaded.builderAssets)
                        .registryAsset(RegistryZipAssembler.CONFIG_TEMPLATE).decodeToString()
                    val content = (context.contentResolver.openInputStream(uri)
                        ?: error("Cannot open the selected config")).bufferedReader(Charsets.UTF_8).use { reader ->
                        val chars = CharArray(262145)
                        var count = 0
                        while (count < chars.size) {
                            val read = reader.read(chars, count, chars.size - count)
                            if (read == -1) break
                            count += read
                        }
                        require(count <= 262144) { "Config file is too large" }
                        String(chars, 0, count)
                    }
                    NikGappsConfigImporter.forProject(content, template, current, loaded)
                }
                save(updated)
                Toast.makeText(context, "Imported ${updated.selectedAppIds.size} packages", Toast.LENGTH_SHORT).show()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Toast.makeText(context, error.message ?: "Unable to import config", Toast.LENGTH_LONG).show()
            } finally { importingConfig = false }
        }
    }
    fun keepAospCounterpart(id: String, keep: Boolean) {
        save(current.copy(keepAospCounterparts = if (keep) current.keepAospCounterparts + id
            else current.keepAospCounterparts - id))
    }
    fun exportConfig() {
        if (metadata == null) return
        scope.launch {
            exportingConfig = true
            try {
                pendingConfig = withContext(Dispatchers.IO) {
                    val loaded = catalogRepository.load(catalogAndroidVersion(current.androidVersion.displayName),
                        current.defaultChannel, current.architecture.value, forceRefresh = true)
                    val template = AndroidBuilderAssetSource(context, loaded.builderAssets)
                        .registryAsset(RegistryZipAssembler.CONFIG_TEMPLATE).decodeToString()
                    NikGappsConfigExporter.forProject(template, current, loaded)
                }
                exportLauncher.launch("${current.name}.config")
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                pendingConfig = null
                Toast.makeText(context, error.message ?: "Unable to prepare config", Toast.LENGTH_LONG).show()
            } finally { exportingConfig = false }
        }
    }
    fun openPackage(id: String) {
        AppDiagnostics.info("navigation", "package_details_opened", mapOf("package" to id))
        navController.navigate(appConfigRoute(projectId, id))
    }
    fun selectPackages(ids: Set<String>, enabled: Boolean, owner: CatalogAppSet? = null) {
        if (!isOnline) {
            Toast.makeText(context, "Internet connection is required to select apps", Toast.LENGTH_LONG).show()
            return
        }
        AppDiagnostics.info("selection", if (enabled) "packages_selected" else "packages_cleared",
            mapOf("project" to projectId.take(8), "count" to ids.size, "appSet" to owner?.id))
        save(selectProjectPackages(current, ids, enabled, owner?.id,
            packageAppSets.mapNotNull { (id, sets) -> sets.firstOrNull()?.id?.let { id to it } }.toMap()))
    }
    fun updateAllSelections(action: String) {
        if (!isOnline) {
            Toast.makeText(context, "Internet connection is required to select apps", Toast.LENGTH_LONG).show()
            return
        }
        val eligibleIds = displayedPackages.mapTo(linkedSetOf()) { it.id }
        val defaults = packageAppSets.mapNotNull { (id, sets) -> sets.firstOrNull()?.id?.let { id to it } }.toMap()
        val updated = when (action) {
            "select" -> {
                val preferred = defaultOwnersForSelectAll(packageAppSets)
                selectProjectPackages(current, preferred.keys, true, defaultOwners = preferred, useDefaultChoices = true)
            }
            "clear" -> selectProjectPackages(current, eligibleIds, false)
            else -> {
                val cleared = selectProjectPackages(current, current.selectedAppIds.intersect(eligibleIds), false)
                selectProjectPackages(cleared, eligibleIds - current.selectedAppIds, true, defaultOwners = defaults)
            }
        }
        save(updated)
        AppDiagnostics.info("selection", "bulk_changed", mapOf("project" to projectId.take(8),
            "operation" to action, "before" to current.selectedAppIds.size, "after" to updated.selectedAppIds.size))
    }
    fun startBuild() {
        val loaded = metadata ?: return
        val appSet = selectedAppSet ?: return
        val quota = BuildQuotaRepository(context)
        val quotaStatus = quota.status()
        if (!quotaStatus.allowed) {
            val resetTime = quotaStatus.resetsAtMillis?.let {
                DateFormat.getTimeFormat(context).format(Date(it))
            }
            Toast.makeText(context, resetTime?.let { "Build limit reached. All slots reset at $it." }
                ?: "Build limit reached.", Toast.LENGTH_LONG).show()
            return
        }
        scope.launch {
            try {
                GitHubBuildAuth.requireBuildAccess()
                progress = ZipBuildProgress(0, current.selectedAppIds.size, "Resolving package versions…")
                val defaultChannel = ReleaseChannel.valueOf(current.defaultChannel.uppercase())
                val overrides = current.channelOverrides.mapValues { ReleaseChannel.valueOf(it.value.uppercase()) }
                val selections = current.selectedAppIds.associateWith { id ->
                    current.selectedPackageAppSets[id] ?: loaded.appSets.appSets.firstOrNull { id in it.packages }?.id
                    ?: throw IllegalArgumentException("No AppSet owns selected package '$id'")
                }
                val resolution = withContext(Dispatchers.IO) { CatalogResolver(loaded.catalog, loaded.appSets, loaded.release).resolveAcrossAppSets(
                    selections, defaultChannel, overrides, current.androidVersion.apiLevel, current.architecture.value) }
                val resolved = resolution.packages
                val visibleTotal = resolved.count { !it.hidden }
                val downloader = ArtifactDownloader(context.cacheDir)
                val validator = PackageZipValidator()
                val artifacts = mutableListOf<ValidatedArtifact>()
                var completedVisible = 0
                resolved.forEach { pkg ->
                    val operation = when {
                        pkg.hidden -> "Downloading required dependency ${pkg.catalogPackage.name}…"
                        else -> "Downloading ${pkg.catalogPackage.name}…"
                    }
                    progress = ZipBuildProgress(completedVisible, visibleTotal, operation)
                    val file = downloader.obtain(pkg) { download -> withContext(Dispatchers.Main) {
                        val percent = download.total?.takeIf { it > 0 }?.let { download.downloaded * 100 / it }
                        progress = ZipBuildProgress(completedVisible, visibleTotal,
                            "${if (pkg.hidden) "Downloading required dependency" else "Downloading"}: ${pkg.catalogPackage.name}${percent?.let { " ($it%)" }.orEmpty()}")
                    } }
                    val artifact = withContext(Dispatchers.IO) { ValidatedArtifact(pkg, file, validator.validate(file, pkg)) }
                    artifacts += artifact
                    if (!pkg.hidden) completedVisible++
                }
                progress = ZipBuildProgress(visibleTotal, visibleTotal, "Assembling the flashable ZIP…")
                GitHubBuildAuth.requireBuildAccess()
                val compressionLevel = verifiedCompressionLevel(BuildCompressionPrefs.compressed,
                    BuildCompressionPrefs.level)
                val output = withContext(Dispatchers.IO) {
                    RegistryZipAssembler(AndroidBuilderAssetSource(context, requireNotNull(registry).builderAssets)).build(
                        File(context.cacheDir, "zip-builds"), BuildRequest(current.androidVersion.displayName,
                            current.androidVersion.apiLevel, current.architecture.value, appSet, defaultChannel,
                            overrides, current.selectedAppIds, packageAppSets = resolution.packageAppSets,
                            timestamp = loaded.release?.createdAt?.let(java.time.Instant::parse) ?: java.time.Instant.now(),
                            releaseId = loaded.release?.id, compressionLevel = compressionLevel,
                            keepAospCounterparts = current.keepAospCounterparts), artifacts)
                }
                try {
                    GitHubBuildAuth.requireBuildAccess()
                } catch (error: Exception) {
                    output.delete()
                    throw error
                }
                val published = withContext(Dispatchers.IO) {
                    ZipPublisher(context).publish(output, current.selectedAppIds.size)
                }
                quota.recordSuccess()
                output.delete()
                progress = null; result = false to "Flashable ZIP created:\n$published"
            } catch (e: Exception) { progress = null; result = true to (e.message ?: "Build failed") }
        }
    }

    LaunchedEffect(autoBuild, metadata) {
        if (autoBuild && !autoBuildConsumed && metadata != null && unavailable.isEmpty()) {
            autoBuildConsumed = true
            startBuild()
        }
    }

    if (registry != null && unavailable.isNotEmpty()) {
        var confirmDelete by remember { mutableStateOf(false) }
        Scaffold(topBar = { TopAppBar(
            title = { Text(current.name) },
            navigationIcon = { IconButton(onClick = navController::navigateUp) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            } }
        ) }) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Text("Outdated project", style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                            Text("Read only · ${current.androidVersion.displayName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Some selected packages are no longer in this release. Your original project " +
                                "is safe, but it can't be edited or built. Duplicate it to continue without them.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Unavailable packages (${unavailable.size})",
                                style = MaterialTheme.typography.titleMedium)
                            unavailable.sorted().forEachIndexed { index, packageId ->
                                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(packageId.split('_').joinToString(" ") { word ->
                                        word.replaceFirstChar { it.uppercase() }
                                    }, style = MaterialTheme.typography.bodyLarge)
                                    Text(packageId, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("What would you like to do?", style = MaterialTheme.typography.titleMedium)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val copy = duplicateCurrentProject(current, registry)
                                    repository.addProject(copy)
                                    navController.navigate(projectRoute(copy.id))
                                }, modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)) {
                                    Text("Duplicate", maxLines = 1)
                                }
                                OutlinedButton(onClick = navController::navigateUp,
                                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)) {
                                    Text("Keep", maxLines = 1)
                                }
                                OutlinedButton(onClick = { confirmDelete = true },
                                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)) {
                                    Text("Delete", maxLines = 1,
                                        color = MaterialTheme.colorScheme.error)
                                }
                            }
                            Text("Duplicate removes these packages from a new project. Keep returns to your " +
                                "projects without changing this one.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false },
            title = { Text("Delete outdated project?") },
            text = { Text("This cannot be undone. You can duplicate it first to keep a supported selection.") },
            confirmButton = { TextButton(onClick = {
                repository.deleteProject(current.id)
                confirmDelete = false
                navController.navigateUp()
            }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })
        return
    }

    Scaffold(topBar = { TopAppBar(title = {
        Text(current.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }, navigationIcon = {
        IconButton(onClick = navController::navigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
    }, actions = {
        IconButton(onClick = {
            if (searchVisible) {
                searchInput = TextFieldValue("")
                searchVisible = false
                keyboard?.hide()
            } else {
                searchVisible = true
            }
            sortExpanded = false
            filterExpanded = false
            notificationsExpanded = false
        }) {
            Icon(
                if (searchVisible) Icons.Default.SearchOff else Icons.Default.Search,
                if (searchVisible) "Close search" else "Search apps"
            )
        }
        IconButton(onClick = {
            notificationsExpanded = !notificationsExpanded
            sortExpanded = false
            filterExpanded = false
        }) {
            val quotaStatus = BuildQuotaRepository(context).status(quotaClock)
            Box(Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.DataUsage,
                    if (notificationsExpanded) "Close project updates" else
                        "Build usage, ${quotaStatus.remaining} of ${quotaStatus.limit} builds available",
                    modifier = Modifier.align(Alignment.Center).size(24.dp)
                )
                if (quotaStatus.remaining < quotaStatus.limit) {
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd),
                        containerColor = if (quotaStatus.remaining == 0) MaterialTheme.colorScheme.error
                            else BadgeDefaults.containerColor,
                        contentColor = if (quotaStatus.remaining == 0) MaterialTheme.colorScheme.onError
                            else MaterialTheme.colorScheme.onError
                    ) {
                        Text(quotaStatus.remaining.toString())
                    }
                }
            }
        }
        IconButton(onClick = {
            sortExpanded = !sortExpanded
            filterExpanded = false
            notificationsExpanded = false
        }) {
            Icon(Icons.AutoMirrored.Filled.Sort, if (sortExpanded) "Close sort options" else "Sort apps")
        }
        IconButton(onClick = {
            filterExpanded = !filterExpanded
            sortExpanded = false
            notificationsExpanded = false
        }) {
            Icon(Icons.Default.FilterAlt, if (filterExpanded) "Close filter options" else "Filter apps")
        }
    }) }, bottomBar = {
        if (metadata != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 448.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (current.selectedAppIds.isEmpty()) {
                        Surface(onClick = { importLauncher.launch(arrayOf("*/*")) },
                            enabled = !importingConfig && !exportingConfig && isOnline,
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer, tonalElevation = 2.dp) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FileUpload, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (importingConfig) "Importing…" else "Import config",
                                    style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }
                        }
                    } else Surface(
                        onClick = {
                            if (!isOnline) {
                                Toast.makeText(
                                    context,
                                    "Internet connection is required before building the ZIP",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                AppDiagnostics.info(
                                    "navigation",
                                    "build_opened",
                                    mapOf(
                                        "project" to projectId.take(8),
                                        "selected" to current.selectedAppIds.size
                                    )
                                )
                                navController.navigate(buildZipRoute(projectId))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Inventory2, null, Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Build ZIP · ${current.selectedAppIds.size}",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    }
                    if (current.selectedAppIds.isNotEmpty()) Surface(onClick = ::exportConfig, enabled = !exportingConfig && !importingConfig,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer, tonalElevation = 2.dp) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileDownload, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (exportingConfig) "Exporting…" else "Export config",
                                style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        }
                    }
                }
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(padding)) {
            AnimatedVisibility(
                visible = sortExpanded || filterExpanded || notificationsExpanded,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth().animateContentSize()
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(when {
                            sortExpanded -> "Sort apps"
                            filterExpanded -> "Filter apps"
                            else -> "Project updates"
                        }, style = MaterialTheme.typography.labelLarge)
                        if (sortExpanded) {
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(48.dp)) {
                                PackageSort.entries.forEachIndexed { index, option ->
                                    SegmentedButton(
                                        selected = packageSort == option,
                                        onClick = { packageSort = option },
                                        shape = SegmentedButtonDefaults.itemShape(index, PackageSort.entries.size)
                                    ) { Text(option.label, maxLines = 1) }
                                }
                            }
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(48.dp)) {
                                listOf(false to "Ascending", true to "Descending").forEachIndexed { index, (descending, label) ->
                                    SegmentedButton(
                                        selected = sortDescending == descending,
                                        onClick = { sortDescending = descending },
                                        shape = SegmentedButtonDefaults.itemShape(index, 2)
                                    ) { Text(label) }
                                }
                            }
                        } else if (filterExpanded) {
                            FilterChip(
                                selected = installedOnly,
                                onClick = { installedOnly = !installedOnly },
                                label = { Text("Installed only") },
                                leadingIcon = { Icon(Icons.Default.PhoneAndroid, null, Modifier.size(16.dp)) },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            )
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(48.dp)) {
                                SelectionFilter.entries.forEachIndexed { index, option ->
                                    SegmentedButton(
                                        selected = selectionFilter == option,
                                        onClick = { selectionFilter = option },
                                        shape = SegmentedButtonDefaults.itemShape(index, SelectionFilter.entries.size)
                                    ) { Text(option.label, maxLines = 1) }
                                }
                            }
                        } else {
                            val releaseDate = metadata?.release?.createdAt?.take(10)
                            val quotaStatus = BuildQuotaRepository(context).status(quotaClock)
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.NewReleases, null, Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(metadata?.release?.let {
                                        "Apps are from release ${releaseDate ?: it.id}."
                                    } ?: "Release information is not available yet.",
                                        style = MaterialTheme.typography.bodyMedium)
                                    val fetchedAt = metadata?.fetchedAtMillis
                                    val nextFetchAt = fetchedAt?.plus(CatalogRepository.CACHE_TTL_MILLIS)
                                    Text(
                                        buildString {
                                            append("App data is cached for 30 minutes. Refresh manually to check now.")
                                            when {
                                                nextFetchAt == null -> Unit
                                                quotaClock >= nextFetchAt -> append(
                                                    " The next screen or build load will check for updates."
                                                )
                                                else -> append(
                                                    " The next network fetch is eligible at ${
                                                        DateFormat.getTimeFormat(context).format(Date(nextFetchAt))
                                                    } and runs when this screen or a build next loads."
                                                )
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Inventory2, null, Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        if (quotaStatus.remaining == 0)
                                            "Build limit reached · 0 of ${quotaStatus.limit} builds available."
                                        else
                                            "${quotaStatus.remaining} of ${quotaStatus.limit} builds available.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (quotaStatus.remaining == 0) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.onSurface
                                    )
                                    val resetsAt = quotaStatus.resetsAtMillis
                                    if (resetsAt != null) {
                                        val resetTime = DateFormat.getTimeFormat(context).format(Date(resetsAt))
                                        val remainingMillis = (resetsAt - quotaClock).coerceAtLeast(0L)
                                        val hours = remainingMillis / 3_600_000L
                                        val minutes = (remainingMillis % 3_600_000L) / 60_000L
                                        Text(
                                            "All ${quotaStatus.limit} slots reset together at $resetTime " +
                                                "(in ${if (hours > 0) "${hours}h " else ""}${minutes}m).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            "Your ${quotaStatus.windowMillis / 3_600_000L}-hour window starts after the first successful build.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (eliteMember) {
                                val nextReset = quotaStatus.nextEliteResetAtMillis
                                Text(
                                    when {
                                        nextReset != null -> "Elite reset available again at " +
                                            DateFormat.getTimeFormat(context).format(Date(nextReset)) + "."
                                        quotaStatus.eliteResetAvailable ->
                                            "Elite benefit: start a new six-hour window with your " +
                                                "${quotaStatus.remaining} remaining builds plus 6."
                                        else -> "Elite reset is unavailable for this six-hour period."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (eliteMember) FilledTonalButton(
                                    onClick = { confirmEliteResetRemaining = quotaStatus.remaining },
                                    enabled = isOnline && quotaStatus.eliteResetAvailable && !resettingEliteQuota,
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) { Text(if (resettingEliteQuota) "Checking Elite status…" else "Reset build window") }
                                FilledTonalButton(
                                    onClick = { metadataRefreshes++ },
                                    enabled = isOnline && !metadataLoading,
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Text(if (metadataLoading) "Refreshing…" else "Refresh app list")
                                }
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = searchVisible,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .focusRequester(searchFocusRequester),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    placeholder = { Text("Search apps") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, "Search apps", tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchInput.text.isNotEmpty()) {
                                searchInput = TextFieldValue("")
                            } else {
                                searchVisible = false
                                keyboard?.hide()
                            }
                        }) {
                            Icon(
                                Icons.Default.Close,
                                if (searchInput.text.isNotEmpty()) "Clear search" else "Close search"
                            )
                        }
                    },
                    supportingText = if (searchQuery.isNotBlank() && sortedPackages.isEmpty()) {{
                        Text("No apps match “${searchQuery.trim()}”")
                    }} else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            // Measuring a header-only/temporarily filtered list clamps the restored scroll index
            // to zero when returning from package details. Wait for both data sources first.
            if (registry == null || catalogPackages.any { it.id !in deviceStatuses }) {
                Box(Modifier.fillMaxWidth().weight(1f).padding(24.dp), contentAlignment = Alignment.Center) {
                    if (loadError != null) Text(loadError.orEmpty(), color = MaterialTheme.colorScheme.error)
                    else CircularProgressIndicator()
                }
            } else LazyColumn(
            state = appsListState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Choose your apps", style = MaterialTheme.typography.titleLarge)
                Text("Tap to select · Press and hold for package info and options.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(true to "AppSets", false to "Packages").forEachIndexed { index, (grouped, label) ->
                        SegmentedButton(selected = appSetView == grouped, onClick = { appSetView = grouped },
                            shape = SegmentedButtonDefaults.itemShape(index, 2,
                                baseShape = RoundedCornerShape(16.dp))) { Text(label) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = !searchVisible,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(interactionSource = null, indication = null,
                            onClick = { summaryExpanded = !summaryExpanded }),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("${current.selectedAppIds.size} apps selected", Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall)
                            Icon(if (summaryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                if (summaryExpanded) "Hide selection" else "Review selection", Modifier.size(20.dp))
                        }
                        AnimatedVisibility(summaryExpanded,
                            enter = expandVertically(tween(220), expandFrom = Alignment.Top) + fadeIn(tween(160)),
                            exit = shrinkVertically(tween(220), shrinkTowards = Alignment.Top) + fadeOut(tween(120))) {
                            Column(Modifier.padding(top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .18f))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = { updateAllSelections("select") },
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(18.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) {
                                        Icon(Icons.Default.SelectAll, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Select", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                                    }
                                    FilledTonalButton(
                                        onClick = { updateAllSelections("clear") },
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(18.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) {
                                        Icon(Icons.Default.Deselect, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Clear", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                                    }
                                    FilledTonalButton(
                                        onClick = { updateAllSelections("invert") },
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(18.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Invert", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                val selectedPackages = displayedPackages.filter { it.id in current.selectedAppIds }
                                selectedPackages.forEachIndexed { index, selectedPkg ->
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Android, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(selectedPkg.name, style = MaterialTheme.typography.bodyMedium)
                                            Text(selectedPkg.versions.values.firstNotNullOfOrNull { it.packageName } ?: selectedPkg.id,
                                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .72f))
                                        }
                                    }
                                    if (index != selectedPackages.lastIndex) HorizontalDivider(
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .14f))
                                }
                            }
                        }
                        }
                    }
                }
                loadError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
                if (metadata == null && loadError == null) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            }
            if (!appSetView) sortedPackages.forEach { pkg ->
                item(key = "package:${pkg.id}") {
                    ProjectPackageCard(pkg, deviceStatuses[pkg.id] ?: RegistryDeviceStatus(false),
                        pkg.id in current.selectedAppIds,
                        onOpen = { openPackage(pkg.id) },
                        keepAosp = pkg.id in current.keepAospCounterparts,
                        onKeepAospChange = { keepAospCounterpart(pkg.id, it) },
                        onSelected = { selectPackages(setOf(pkg.id), it) })
                }
            } else {
                val groups = registry?.appSets?.appSets.orEmpty().map { set ->
                    set to sortedPackages.filter { pkg -> set in packageAppSets[pkg.id].orEmpty() &&
                        when (selectionFilter) {
                            SelectionFilter.BOTH -> true
                            SelectionFilter.SELECTED -> isSelectedInAppSet(current, pkg.id, set.id)
                            SelectionFilter.UNSELECTED -> !isSelectedInAppSet(current, pkg.id, set.id)
                        }
                    }
                }.filter { it.second.isNotEmpty() }
                val comparator = when (packageSort) {
                    PackageSort.NAME -> compareBy<Pair<CatalogAppSet, List<CatalogPackage>>> { it.first.name.lowercase() }
                    PackageSort.INSTALLED -> compareBy { group: Pair<CatalogAppSet, List<CatalogPackage>> ->
                        group.second.count { deviceStatuses[it.id]?.installed == true }
                    }
                    PackageSort.SELECTED -> compareBy { group: Pair<CatalogAppSet, List<CatalogPackage>> ->
                        group.second.count { isSelectedInAppSet(current, it.id, group.first.id) }
                    }
                }
                val sortedGroups = groups.sortedWith(comparator.thenBy { it.first.name.lowercase() })
                    .let { if (sortDescending) it.reversed() else it }
                sortedGroups.forEach { (set, matches) ->
                    val ids = registry?.catalog?.publicPackages(set).orEmpty().mapTo(linkedSetOf()) { it.id }
                    val selectedCount = ids.count { isSelectedInAppSet(current, it, set.id) }
                    val expanded = set.id in expandedAppSets || searchQuery.isNotBlank()
                    item(key = "appset:${set.id}") {
                        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                            Row(Modifier.fillMaxWidth().clickable(interactionSource = null, indication = null, onClick = {
                                expandedAppSets = if (set.id in expandedAppSets) expandedAppSets - set.id
                                    else expandedAppSets + set.id
                            }).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(set.name, style = MaterialTheme.typography.titleSmall)
                                    Text("$selectedCount of ${ids.size} packages selected",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TriStateCheckbox(state = when {
                                    selectedCount == 0 -> ToggleableState.Off
                                    selectedCount == ids.size -> ToggleableState.On
                                    else -> ToggleableState.Indeterminate
                                }, enabled = isOnline,
                                    onClick = { selectPackages(ids, selectedCount != ids.size, set) })
                                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    if (expanded) "Collapse ${set.name}" else "Expand ${set.name}")
                            }
                        }
                    }
                    if (expanded) matches.forEach { pkg ->
                        item(key = "appset:${set.id}:${pkg.id}") {
                            ProjectPackageCard(pkg, deviceStatuses[pkg.id] ?: RegistryDeviceStatus(false),
                                isSelectedInAppSet(current, pkg.id, set.id),
                                onOpen = { openPackage(pkg.id) },
                                keepAosp = pkg.id in current.keepAospCounterparts,
                                onKeepAospChange = { keepAospCounterpart(pkg.id, it) },
                                onSelected = { selectPackages(setOf(pkg.id), it, set) },
                                modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
            }
        }
        }
    }
    confirmEliteResetRemaining?.let { remaining ->
        val newLimit = eliteWindowLimit(remaining)
        AlertDialog(
            onDismissRequest = { confirmEliteResetRemaining = null },
            title = { Text("Reset build window?") },
            text = { Text("You have $remaining builds remaining. Your new six-hour window will " +
                "start with $newLimit builds ($remaining remaining + 6 Elite builds). " +
                "You cannot reset it again for six hours.") },
            confirmButton = { TextButton(onClick = {
                confirmEliteResetRemaining = null
                val username = GithubPrefs.username
                resettingEliteQuota = true
                scope.launch {
                    try {
                        check(GithubPrefs.token.isNotBlank() &&
                            GithubPrefs.username.equals(username, ignoreCase = true)) {
                            "GitHub sign-in changed; please try again"
                        }
                        BuildQuotaRepository(context).resetForElite(username, remaining)
                        quotaClock = System.currentTimeMillis()
                        Toast.makeText(context, "$newLimit builds available in the new window",
                            Toast.LENGTH_SHORT).show()
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        quotaClock = System.currentTimeMillis()
                        Toast.makeText(context, error.message ?: "Could not reset the build window",
                            Toast.LENGTH_LONG).show()
                    } finally {
                        resettingEliteQuota = false
                    }
                }
            }) { Text("Reset window") } },
            dismissButton = { TextButton(onClick = { confirmEliteResetRemaining = null }) { Text("Cancel") } }
        )
    }
    progress?.let { p -> AlertDialog({}, title = { Text("Building flashable ZIP") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LinearProgressIndicator(progress = { p.fraction }, modifier = Modifier.fillMaxWidth()); Text(p.message); Text("${p.completed} of ${p.total} packages")
    } }, confirmButton = {}) }
    result?.let { (failed, message) -> AlertDialog({ result = null }, title = { Text(if (failed) "Build failed" else "Build complete") },
        text = { Text(message) }, confirmButton = { TextButton({ result = null }) { Text("OK") } }) }
}

@Composable
private fun ProjectPackageCard(pkg: CatalogPackage, device: RegistryDeviceStatus, selected: Boolean,
    onOpen: () -> Unit, onSelected: (Boolean) -> Unit, keepAosp: Boolean,
    onKeepAospChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    var menuOpen by remember(pkg.id) { mutableStateOf(false) }
    var pressPosition by remember(pkg.id) { mutableStateOf(Offset.Zero) }
    Box(modifier.fillMaxWidth()) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = if (selected)
            MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
        Box(Modifier.fillMaxWidth()) {
            ProjectPackageRow(pkg, device, selected, onSelected = onSelected,
                onPressPosition = { pressPosition = it }, onLongPress = { menuOpen = true })
            if (selected && keepAosp) {
                Box(Modifier.matchParentSize()) {
                    Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(4.dp)
                        .background(MaterialTheme.colorScheme.error))
                }
            }
        }
    }
    Box(Modifier.offset { IntOffset(pressPosition.x.roundToInt(), pressPosition.y.roundToInt()) }.size(1.dp)) {
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false },
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        if (selected) {
            DropdownMenuItem(
                text = { Text(if (keepAosp) "Delete AOSP Counterpart" else "Keep AOSP Counterpart") },
                leadingIcon = { Icon(if (keepAosp) Icons.Default.DeleteOutline else Icons.Default.Shield, null) },
                onClick = { menuOpen = false; onKeepAospChange(!keepAosp) }
            )
        }
        DropdownMenuItem(text = { Text("Show info") },
            leadingIcon = { Icon(Icons.Default.Info, null) },
            onClick = { menuOpen = false; onOpen() })
    }
    }
    }
}

@Composable
private fun RegistryAppRow(pkg: CatalogPackage, source: AppSource, device: RegistryDeviceStatus, selected: Boolean,
    channel: String, memberAppSets: List<CatalogAppSet>, selectedAppSetId: String?, expanded: Boolean,
    onExpand: () -> Unit,
    onSelected: (Boolean) -> Unit, onSource: (AppSource) -> Unit, onAppSet: (CatalogAppSet) -> Unit,
    onChannel: (ReleaseChannel) -> Unit) {
    val enabled = source == AppSource.GITLAB || device.installed
    val catalogVersion = pkg.channels[channel]?.let(pkg.versions::get) ?: pkg.versions.values.firstOrNull()
    val deviceIsNewer = device.versionCode != null && catalogVersion != null && device.versionCode > catalogVersion.versionCode
    val availableChannels = ReleaseChannel.entries.filter { it.wireName in pkg.channels }
    val selectedChannel = availableChannels.firstOrNull { it.wireName == channel } ?: availableChannels.firstOrNull()
    val nextChannel = selectedChannel?.takeIf { availableChannels.size > 1 }
        ?.let { availableChannels[(availableChannels.indexOf(it) + 1) % availableChannels.size] }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onExpand), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Android, null) } }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(pkg.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(catalogVersion?.packageName ?: pkg.id, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Checkbox(selected, onSelected, enabled = enabled)
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                if (expanded) "Collapse ${pkg.name}" else "Configure ${pkg.name}")
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                catalogVersion?.let { version ->
                    val payloadSize = version.install?.payloadSize
                        ?: version.files.sumOf { it.size }
                    val fileSummary = version.files.groupingBy { it.type }.eachCount().entries
                        .sortedBy { it.key }.joinToString(" · ") { "${it.value} ${it.key}" }
                    Text("Package details", style = MaterialTheme.typography.labelLarge)
                    Text(buildString {
                        append("Version ${version.versionName} (${version.versionCode})")
                        append(" · API ${version.android.minApi ?: "any"}–${version.android.maxApi ?: "current"}")
                        append(" · ${payloadSize / 1_048_576.0f} MiB")
                    }, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (fileSummary.isNotBlank()) Text(fileSummary, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Installs to ${version.defaultPartition}; ${version.files.size} verified files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Text("Package source", style = MaterialTheme.typography.labelLarge)
                Text("Select which version should be placed in the flashable ZIP.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SourceTile(title = "Catalog", version = catalogVersion?.versionName, icon = Icons.Default.CloudDownload,
                        selected = source == AppSource.GITLAB, enabled = true, modifier = Modifier.weight(1f),
                        supportingText = if (deviceIsNewer && source == AppSource.GITLAB) "Newer version on device" else null) {
                        onSource(AppSource.GITLAB)
                    }
                    SourceTile(title = "Device", version = device.versionName, icon = Icons.Default.PhoneAndroid,
                        selected = source == AppSource.DEVICE, enabled = device.installed, modifier = Modifier.weight(1f),
                        supportingText = if (device.installed) null else "Not installed") {
                        onSource(AppSource.DEVICE)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("AppSet", style = MaterialTheme.typography.labelLarge)
                        Text("Highlighted AppSet owns this package", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            memberAppSets.forEach { appSet ->
                                FilterChip(selected = appSet.id == selectedAppSetId,
                                    onClick = { onAppSet(appSet) }, label = { Text(appSet.name) },
                                    leadingIcon = if (appSet.id == selectedAppSetId) {{
                                        Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                                    }} else null)
                            }
                        }
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Release channel", style = MaterialTheme.typography.labelLarge)
                        FilledTonalButton(onClick = { nextChannel?.let(onChannel) }, enabled = nextChannel != null,
                            modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp)) {
                            Icon(Icons.Default.Sync, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(selectedChannel?.wireName?.replaceFirstChar { it.uppercase() } ?: "Unavailable",
                                maxLines = 1)
                        }
                        if (availableChannels.size > 1) Text("Tap to switch channel", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceTile(title: String, version: String?, icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean, enabled: Boolean, modifier: Modifier = Modifier, supportingText: String? = null,
    onClick: () -> Unit) {
    Card(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 112.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(20.dp))
                Spacer(Modifier.weight(1f))
                if (selected) Icon(Icons.Default.CheckCircle, "Selected source", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(version?.let { "v$it" } ?: "Version unavailable", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            supportingText?.let { Text(it, style = MaterialTheme.typography.labelSmall,
                color = if (enabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ProjectPackageRow(pkg: CatalogPackage, device: RegistryDeviceStatus,
    selected: Boolean, onSelected: (Boolean) -> Unit, onPressPosition: (Offset) -> Unit, onLongPress: () -> Unit) {
    val version = pkg.versions.values.firstOrNull()
    Row(Modifier.fillMaxWidth().pointerInput(pkg.id) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            onPressPosition(down.position)
            waitForUpOrCancellation(pass = PointerEventPass.Initial)
        }
    }.combinedClickable(onClick = { onSelected(!selected) },
        onLongClick = onLongPress, onLongClickLabel = "Package options")
        .semantics { role = Role.Checkbox; toggleableState = if (selected) ToggleableState.On else ToggleableState.Off }
        .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(56.dp).height(44.dp)) {
        Surface(Modifier.size(32.dp).align(Alignment.TopCenter), shape = RoundedCornerShape(10.dp),
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Android, null, Modifier.size(20.dp)) }
        }
        if (device.installed) Surface(Modifier.align(Alignment.BottomCenter), shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
            Text("Installed", Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp))
        }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(pkg.name, style = MaterialTheme.typography.titleSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(version?.packageName ?: pkg.id, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

package com.nikgapps.app.presentation.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nikgapps.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

private data class AnalyticsEvent(
    val id: String, val timestamp: Date, val packageCount: Int, val zipName: String,
    val deviceModel: String, val deviceCode: String, val sizeBytes: Long, val location: String,
    val conflictResolution: String, val distinctId: String
)
private data class AnalyticsUser(
    val distinctId: String, val zipCount: Int, val lastCreated: Date?, val deviceModel: String,
    val deviceCode: String
)
private data class AnalyticsDashboard(val events: List<AnalyticsEvent>, val users: List<AnalyticsUser>)
private enum class BuildFilterCategory(val label: String) { DEVICE("Device"), CODE("Device code"), LOCATION("Location"), CONFLICT("Conflict") }

private class AnalyticsApiException(message: String) : Exception(message)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardScreen() {
    val personalKey = BuildConfig.POSTHOG_PERSONAL_API_KEY.trim()
    val projectId = BuildConfig.POSTHOG_PROJECT_ID.trim()
    val configured = BuildConfig.DEBUG && personalKey.isNotBlank() && projectId.isNotBlank()
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build() }
    val events = remember { mutableStateListOf<AnalyticsEvent>() }
    val users = remember { mutableStateListOf<AnalyticsUser>() }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedSection by remember { mutableIntStateOf(0) }
    var filterCategory by remember { mutableStateOf(BuildFilterCategory.DEVICE) }
    var filterValue by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    val visibleEvents by remember {
        derivedStateOf {
            filterValue?.let { selected -> events.filter { it.filterValue(filterCategory) == selected } } ?: events
        }
    }

    fun load() {
        if (!configured || loading) return
        scope.launch {
            loading = true; error = null
            try {
                val dashboard = withContext(Dispatchers.IO) { fetchAnalyticsDashboard(client) }
                events.clear(); events += dashboard.events
                users.clear(); users += dashboard.users
            } catch (e: Exception) { error = e.message ?: "Unable to load PostHog activity" }
            finally { loading = false }
        }
    }

    LaunchedEffect(configured) { if (configured) load() }
    if (showFilters) {
        ModalBottomSheet(onDismissRequest = { showFilters = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            AnalyticsFilterSheet(
                events = events, category = filterCategory, selectedValue = filterValue,
                onCategorySelected = { filterCategory = it; filterValue = null },
                onValueSelected = { filterValue = it; showFilters = false }
            )
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Column {
        Text("Analytics")
        Text("PostHog activity · zip_creation_succeeded", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    } }, actions = {
        IconButton(onClick = { load() }, enabled = configured && !loading) {
            Icon(Icons.Default.Refresh, "Refresh activity")
        }
    }) }) { padding ->
        if (!configured) AnalyticsSetupState(Modifier.fillMaxSize().padding(padding))
        else Column(Modifier.fillMaxSize().padding(padding)) {
            if (loading && events.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
            AnalyticsSummary(visibleEvents)
            PrimaryTabRow(selectedTabIndex = selectedSection) {
                Tab(selected = selectedSection == 0, onClick = { selectedSection = 0 },
                    text = { Text("Builds") }, icon = { Icon(Icons.Default.Archive, null, Modifier.size(18.dp)) })
                Tab(selected = selectedSection == 1, onClick = { selectedSection = 1 },
                    text = { Text("Users (${users.size})") }, icon = { Icon(Icons.Default.People, null, Modifier.size(18.dp)) })
            }
            if (selectedSection == 0 && events.isNotEmpty()) AnalyticsFilterBar(
                totalCount = events.size, visibleCount = visibleEvents.size, category = filterCategory,
                selectedValue = filterValue, onOpen = { showFilters = true }, onClear = { filterValue = null }
            )
            AnimatedContent(error, label = "analytics-error") { message ->
                if (message != null) Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { load() }) { Text("Retry") }
                    }
                }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedSection == 0) {
                    itemsIndexed(visibleEvents, key = { _, item -> item.id }) { index, item ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (index == 0 || dayLabel(visibleEvents[index - 1].timestamp) != dayLabel(item.timestamp)) {
                                Text(dayLabel(item.timestamp), style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            AnalyticsEventCard(item)
                        }
                    }
                } else {
                    itemsIndexed(users, key = { _, item -> item.distinctId }) { index, user ->
                        AnalyticsUserCard(index + 1, user)
                    }
                }
                if ((selectedSection == 0 && visibleEvents.isEmpty() || selectedSection == 1 && users.isEmpty()) &&
                    !loading && error == null) item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (selectedSection == 0) "No ZIP creation events found" else "No users found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable private fun AnalyticsFilterBar(
    totalCount: Int, visibleCount: Int, category: BuildFilterCategory, selectedValue: String?,
    onOpen: () -> Unit, onClear: () -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 14.dp)) {
            Icon(Icons.Default.FilterList, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(selectedValue?.let { "${category.label}: $it" } ?: "Filter builds", Modifier.weight(1f),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$visibleCount/$totalCount", style = MaterialTheme.typography.labelMedium)
        }
        if (selectedValue != null) TextButton(onClick = onClear) { Text("Clear") }
    }
}

@Composable private fun AnalyticsFilterSheet(
    events: List<AnalyticsEvent>, category: BuildFilterCategory, selectedValue: String?,
    onCategorySelected: (BuildFilterCategory) -> Unit, onValueSelected: (String?) -> Unit
) {
    val counts = remember(events, category) {
        events.groupingBy { it.filterValue(category) }.eachCount().entries.sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }
        )
    }
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.82f).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text("Filter builds", style = MaterialTheme.typography.headlineSmall)
            Text("Choose a category, then select a value", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BuildFilterCategory.entries.forEach { option ->
                    FilterChip(selected = category == option, onClick = { onCategorySelected(option) },
                        label = { Text(option.label) })
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
            item {
                FilterValueRow("All", events.size, selectedValue == null) { onValueSelected(null) }
            }
            itemsIndexed(counts, key = { _, item -> item.key }) { _, (value, count) ->
                FilterValueRow(value.ifBlank { "Unknown" }, count, selectedValue == value) { onValueSelected(value) }
            }
        }
    }
}

@Composable private fun FilterValueRow(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text("$count ZIP ${if (count == 1) "build" else "builds"}") },
        trailingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    )
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable private fun AnalyticsSummary(events: List<AnalyticsEvent>) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryCard("ZIP builds", events.size.toString(), Icons.Default.Archive, Modifier.weight(1f))
        SummaryCard("Packages", events.sumOf { it.packageCount }.toString(), Icons.Default.Inventory2, Modifier.weight(1f))
    }
}

@Composable private fun SummaryCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(22.dp)); Spacer(Modifier.width(10.dp)); Column {
                Text(value, style = MaterialTheme.typography.titleLarge); Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable private fun AnalyticsEventCard(event: AnalyticsEvent) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(38.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Archive, null, Modifier.size(20.dp)) }
                }
                Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) {
                    Text(event.zipName, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(relativeTime(event.timestamp), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth()) {
                EventValue("Packages", event.packageCount.toString(), Modifier.weight(1f))
                EventValue("Device", deviceLabel(event.deviceModel, event.deviceCode), Modifier.weight(1.5f))
                EventValue("Time", timeLabel(event.timestamp), Modifier.weight(1f))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth()) {
                EventValue("Size", formatBytes(event.sizeBytes), Modifier.weight(1f))
                EventValue("Conflict", event.conflictResolution.ifBlank { "—" }.replaceFirstChar { it.uppercase() }, Modifier.weight(1f))
            }
            EventValue("Location", event.location.ifBlank { "Downloads/NikGapps" }, Modifier.fillMaxWidth())
        }
    }
}

@Composable private fun AnalyticsUserCard(position: Int, user: AnalyticsUser) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(42.dp), shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer) {
                Box(contentAlignment = Alignment.Center) { Text(position.toString(), style = MaterialTheme.typography.titleSmall) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(deviceLabel(user.deviceModel, user.deviceCode), style = MaterialTheme.typography.titleSmall)
                Text(user.distinctId, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                user.lastCreated?.let { Text("Last build ${relativeTime(it)}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(user.zipCount.toString(), style = MaterialTheme.typography.titleMedium)
                    Text("ZIPs", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable private fun EventValue(label: String, value: String, modifier: Modifier) { Column(modifier) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
} }

@Composable private fun AnalyticsSetupState(modifier: Modifier = Modifier) {
    Box(modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(Modifier.size(72.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Analytics, null, Modifier.size(34.dp)) }
            }
            Text("Connect native analytics", style = MaterialTheme.typography.titleLarge)
            Text("Set POSTHOG_PERSONAL_API_KEY and POSTHOG_PROJECT_ID for a local debug build. Analytics is hidden and credentials are empty in release builds.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun fetchAnalyticsDashboard(client: OkHttpClient): AnalyticsDashboard {
    // PostHog's raw /events endpoint returns complete event payloads and can take
    // longer than the screen's timeout. HogQL asks only for the fields rendered here.
    val eventRows = executeAllEventRows(client)
    val events = eventRows.mapNotNull { row ->
        val timestampText = row.getOrNull(1)?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val timestamp = parseTimestamp(timestampText) ?: return@mapNotNull null
        val zipName = row.getOrNull(2)?.jsonPrimitive?.contentOrNull ?: "Unnamed ZIP"
        AnalyticsEvent(
            id = row.getOrNull(0)?.jsonPrimitive?.contentOrNull ?: "$timestampText-$zipName",
            timestamp = timestamp,
            packageCount = row.numberAsInt(3),
            zipName = zipName,
            deviceModel = row.text(4),
            deviceCode = row.text(5),
            sizeBytes = row.numberAsLong(6),
            location = row.text(7),
            conflictResolution = row.text(8),
            distinctId = row.text(9)
        )
    }
    val users = events.filter { it.distinctId.isNotBlank() }.groupBy { it.distinctId }.map { (distinctId, builds) ->
        val latest = builds.maxBy { it.timestamp }
        AnalyticsUser(distinctId, builds.size, latest.timestamp, latest.deviceModel, latest.deviceCode)
    }.sortedWith(compareByDescending<AnalyticsUser> { it.zipCount }.thenByDescending { it.lastCreated })
    return AnalyticsDashboard(events, users)
}

private fun executeAllEventRows(client: OkHttpClient): List<JsonArray> {
    val pageSize = 10_000
    val rows = mutableListOf<JsonArray>()
    var cursorTimestamp: String? = null
    var cursorUuid: String? = null
    do {
        val cursorClause = if (cursorTimestamp != null && cursorUuid != null) """
            AND (timestamp < parseDateTimeBestEffort('${cursorTimestamp.sqlLiteral()}')
                 OR (timestamp = parseDateTimeBestEffort('${cursorTimestamp.sqlLiteral()}')
                     AND uuid < toUUID('${cursorUuid.sqlLiteral()}')))
        """.trimIndent() else ""
        val query = """
            SELECT uuid, timestamp, properties.zip_name, properties.package_count,
                   coalesce(properties.device_model, properties.${'$'}device_model, '') AS device_model,
                   coalesce(properties.device_code, properties.${'$'}device_name, '') AS device_code,
                   properties.size_bytes, coalesce(properties.location, 'Downloads/NikGapps'),
                   properties.conflict_resolution, distinct_id
            FROM events
            WHERE event = 'zip_creation_succeeded'
            $cursorClause
            ORDER BY timestamp DESC, uuid DESC
            LIMIT $pageSize
        """.trimIndent()
        val page = executeHogQl(client, query)
        rows += page
        cursorTimestamp = page.lastOrNull()?.text(1)
        cursorUuid = page.lastOrNull()?.text(0)
    } while (page.size == pageSize)
    return rows
}

private fun String?.sqlLiteral() = this.orEmpty().replace("'", "''")

private fun executeHogQl(client: OkHttpClient, query: String): List<JsonArray> {
    val url = "${BuildConfig.POSTHOG_API_HOST}/api/projects/${BuildConfig.POSTHOG_PROJECT_ID}/query/"
    val payload = buildJsonObject {
        putJsonObject("query") {
            put("kind", "HogQLQuery")
            put("query", query)
        }
    }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    client.newCall(Request.Builder().url(url).header("Authorization", "Bearer ${BuildConfig.POSTHOG_PERSONAL_API_KEY}")
        .header("Accept", "application/json").post(payload).build()).execute().use { response ->
        val body = response.body.string()
        if (!response.isSuccessful) throw AnalyticsApiException(postHogErrorMessage(response.code, body))
        val root = Json.parseToJsonElement(body).jsonObject
        return root["results"]?.jsonArray.orEmpty().map { it.jsonArray }
    }
}

private fun JsonArray.text(index: Int) = getOrNull(index)?.jsonPrimitive?.contentOrNull.orEmpty()
private fun JsonArray.numberAsInt(index: Int) = getOrNull(index)?.jsonPrimitive?.let {
    it.intOrNull ?: it.doubleOrNull?.toInt()
} ?: 0
private fun JsonArray.numberAsLong(index: Int) = getOrNull(index)?.jsonPrimitive?.let {
    it.longOrNull ?: it.doubleOrNull?.toLong()
} ?: 0L
private fun AnalyticsEvent.filterValue(category: BuildFilterCategory) = when (category) {
    BuildFilterCategory.DEVICE -> deviceModel.ifBlank { "Unknown" }
    BuildFilterCategory.CODE -> deviceCode.ifBlank { "Unknown" }
    BuildFilterCategory.LOCATION -> location.ifBlank { "Downloads/NikGapps" }
    BuildFilterCategory.CONFLICT -> conflictResolution.ifBlank { "Unknown" }.replaceFirstChar { it.uppercase() }
}

private fun postHogErrorMessage(status: Int, body: String): String {
    val detail = runCatching {
        val root = Json.parseToJsonElement(body).jsonObject
        root["detail"]?.jsonPrimitive?.contentOrNull
            ?: root["error"]?.jsonPrimitive?.contentOrNull
            ?: root["message"]?.jsonPrimitive?.contentOrNull
    }.getOrNull()?.take(180)
    val key = BuildConfig.POSTHOG_PERSONAL_API_KEY
    return when (status) {
        401 -> when {
            key.startsWith("phc_") -> "A PostHog project ingestion key (phc_) was supplied. Create and use a personal API key (phx_) instead."
            !key.startsWith("phx_") -> "PostHog rejected the credential. Verify that POSTHOG_PERSONAL_API_KEY contains a current personal API key and no quotes or spaces."
            else -> "PostHog rejected this personal key. Verify POSTHOG_API_HOST matches the key's US/EU region and that the key has access to project ${BuildConfig.POSTHOG_PROJECT_ID}."
        }
        403 -> "The personal API key is authenticated but lacks permission. Add query:read/project:read access for project ${BuildConfig.POSTHOG_PROJECT_ID}."
        404 -> "PostHog project ${BuildConfig.POSTHOG_PROJECT_ID} was not found on ${BuildConfig.POSTHOG_API_HOST}. Check the project ID and region."
        else -> "PostHog returned HTTP $status${detail?.let { ": $it" }.orEmpty()}"
    }
}

private val isoFormats = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
private fun parseTimestamp(value: String): Date? = isoFormats.firstNotNullOfOrNull { pattern -> runCatching {
    SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(value)
}.getOrNull() }
private fun dayLabel(date: Date) = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date)
private fun timeLabel(date: Date) = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
private fun formatBytes(bytes: Long): String = when {
    bytes <= 0L -> "Unknown"
    bytes >= 1_073_741_824L -> String.format(Locale.getDefault(), "%.2f GB", bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1_048_576.0)
    else -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
}
private fun deviceLabel(model: String, code: String): String = when {
    model.isNotBlank() && code.isNotBlank() -> "$model · $code"
    model.isNotBlank() -> model
    code.isNotBlank() -> code
    else -> "Unknown device"
}
private fun relativeTime(date: Date): String { val seconds = ((System.currentTimeMillis() - date.time) / 1000).coerceAtLeast(0)
    return when { seconds < 60 -> "just now"; seconds < 3600 -> "${seconds / 60}m ago";
        seconds < 86400 -> "${seconds / 3600}h ago"; else -> "${seconds / 86400}d ago" } }

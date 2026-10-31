package com.nikgapps.app.presentation.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nikgapps.app.data.AppFeature
import com.nikgapps.app.data.AppFeatureCatalog
import kotlinx.coroutines.launch

@Composable
fun AppFeatureGuideDialog(screen: String, onDismiss: () -> Unit) {
    var relevant by remember(screen) { mutableStateOf(true) }
    val tabState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Discover NikGapps", style = MaterialTheme.typography.titleLarge)
                        Text("Help for $screen", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close help")
                    }
                }
                Box(Modifier.weight(1f)) {
                    tabState.SaveableStateProvider(if (relevant) "relevant:$screen" else "help") {
                        if (relevant) RelevantHelp(screen) else CompleteHelpGuide()
                    }
                }
                HorizontalDivider()
                Surface(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Help" to false, "Relevant" to true).forEach { (label, value) ->
                            val active = relevant == value
                            val color by animateColorAsState(
                                if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                tween(180), label = "Help tab color")
                            Surface(Modifier.weight(1f).height(40.dp).selectable(
                                selected = active, role = androidx.compose.ui.semantics.Role.Tab,
                                onClick = { relevant = value }), shape = RoundedCornerShape(10.dp),
                                color = color,
                                contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(label, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelevantHelp(screen: String) {
    val sections = com.nikgapps.app.data.AppFeatureCatalog.relevantPages(screen)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        sections.forEach { page ->
            item(key = page.title) {
                Text(page.title, style = MaterialTheme.typography.titleLarge)
                Text(page.description, Modifier.padding(top = 4.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(page.features, key = { page.title + it.title }) { FeatureGuideCard(it) }
        }
    }
}

@Composable
private fun CompleteHelpGuide() {
    val pages = AppFeatureCatalog.pages
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        Text("${pages[pager.currentPage].title} · ${pager.currentPage + 1} of ${pages.size}",
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                HorizontalPager(pager, Modifier.fillMaxWidth().weight(1f), key = { pages[it].title }) { index ->
                    val page = pages[index]
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Text(page.title, style = MaterialTheme.typography.titleLarge)
                            Text(page.description, Modifier.padding(top = 4.dp, bottom = 4.dp),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        items(page.features, key = { it.title }) { feature -> FeatureGuideCard(feature) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    pages.forEachIndexed { index, page ->
                        Box(Modifier.size(48.dp).clickable(onClickLabel = "Go to ${page.title}") {
                            scope.launch { pager.animateScrollToPage(index) }
                        }, contentAlignment = Alignment.Center) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(
                                if (index == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                        }
                    }
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                        enabled = pager.currentPage > 0, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Previous") }
                    FilledTonalButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                        enabled = pager.currentPage < pages.lastIndex, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Next") }
                }
    }
}

@Composable
private fun FeatureGuideCard(feature: AppFeature) {
    var expanded by rememberSaveable(feature.title) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(280), label = "featureArrow")
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(interactionSource = null, indication = null,
                onClickLabel = if (expanded) "Collapse ${feature.title}" else "Expand ${feature.title}") { expanded = !expanded }
                .padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(feature.title, style = MaterialTheme.typography.titleSmall)
                    Text(feature.summary, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (feature.elite) Icon(Icons.Default.Star, "Elite feature", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Icon(Icons.Default.ExpandMore, null, Modifier.rotate(rotation))
            }
            AnimatedVisibility(expanded,
                enter = expandVertically(tween(280), expandFrom = Alignment.Top) + fadeIn(tween(220)),
                exit = shrinkVertically(tween(280), shrinkTowards = Alignment.Top) + fadeOut(tween(220))) {
                Column {
                    HorizontalDivider()
                    Text(feature.details, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

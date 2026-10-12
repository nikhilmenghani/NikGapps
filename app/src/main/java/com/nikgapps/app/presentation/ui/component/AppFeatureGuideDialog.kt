package com.nikgapps.app.presentation.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nikgapps.R
import com.nikgapps.app.data.AppFeature
import com.nikgapps.app.data.AppFeatureCatalog
import kotlinx.coroutines.launch

@Composable
fun AppFeatureGuideDialog(onDismiss: () -> Unit) {
    val pages = AppFeatureCatalog.pages
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.94f).height((LocalConfiguration.current.screenHeightDp * .85f).dp),
            shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column {
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.nikgapps_logo), null, Modifier.size(48.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Discover NikGapps", style = MaterialTheme.typography.titleLarge)
                        Text("${pages[pager.currentPage].title} · ${pager.currentPage + 1} of ${pages.size}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
                    FilledTonalButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Close") }
                }
            }
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

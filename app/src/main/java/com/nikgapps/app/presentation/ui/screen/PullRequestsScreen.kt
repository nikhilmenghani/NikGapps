package com.nikgapps.app.presentation.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nikgapps.app.data.GithubPrefs
import com.nikgapps.app.utils.network.GitHubPullRequest
import com.nikgapps.app.utils.network.GitHubPullRequestSearch
import com.nikgapps.app.utils.network.GitHubPullRequests

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRequestsScreen() {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<GitHubPullRequestSearch?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshKey, GithubPrefs.token, GithubPrefs.username) {
        loading = true
        error = null
        try {
            result = GitHubPullRequests.createdBy(GithubPrefs.token, GithubPrefs.username)
        } catch (failure: Exception) {
            error = failure.message ?: "Could not load your pull requests."
        } finally {
            loading = false
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Build requests") },
            actions = {
                IconButton(onClick = { refreshKey++ }, enabled = !loading) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            },
        )
    }) { padding ->
        when {
            loading && result == null -> Box(
                Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            error != null && result == null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.ErrorOutline, null, Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                Text(error.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { refreshKey++ }) { Text("Try again") }
            }

            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                result?.let { search ->
                    item {
                        SummaryCard(search, GithubPrefs.username)
                        if (error != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (search.pullRequests.isEmpty()) {
                        item { EmptyPullRequests() }
                    } else {
                        items(search.pullRequests, key = { it.number }) { pullRequest ->
                            PullRequestCard(pullRequest) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pullRequest.url)))
                            }
                        }
                        if (search.totalCount > search.pullRequests.size) {
                            item {
                                Text("Showing the latest ${search.pullRequests.size} requests.",
                                    Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(search: GitHubPullRequestSearch, username: String) {
    Surface(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountTree, null, Modifier.size(36.dp))
            Column(Modifier.padding(start = 16.dp)) {
                Text(search.totalCount.toString(), style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
                Text(if (search.totalCount == 1) "Pull request by @$username"
                    else "Pull requests by @$username", style = MaterialTheme.typography.bodyMedium)
                if (search.incomplete) Text("GitHub marked these results as incomplete",
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun EmptyPullRequests() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.AccountTree, null, Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text("No build requests yet", style = MaterialTheme.typography.titleMedium)
        Text("Pull requests you create in nikgapps/config will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PullRequestCard(pullRequest: GitHubPullRequest, onOpen: () -> Unit) {
    val label = when {
        pullRequest.isDraft -> "Draft"
        pullRequest.isMerged -> "Merged"
        pullRequest.state == "open" -> "Open"
        else -> "Closed"
    }
    Surface(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("#${pullRequest.number}", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text(pullRequest.title, style = MaterialTheme.typography.titleMedium,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.AutoMirrored.Filled.OpenInNew, "Open on GitHub", Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onOpen, label = { Text(label) }, leadingIcon = {
                    Icon(if (pullRequest.isMerged || pullRequest.state == "open") Icons.Default.CheckCircle
                        else Icons.Default.Schedule, null, Modifier.size(16.dp))
                })
                Text("Created ${pullRequest.createdAt.toDisplayDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun String.toDisplayDate(): String =
    takeIf { length >= 10 }?.substring(0, 10).orEmpty()

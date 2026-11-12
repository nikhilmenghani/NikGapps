package com.nikgapps.app.presentation.ui.component.containers

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nikgapps.app.data.GithubPrefs
import com.nikgapps.app.presentation.ui.component.items.PreferenceItem
import com.nikgapps.app.utils.network.GitHubDeviceAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun GitHubTokenPreference() {
    var dialogOpen by remember { mutableStateOf(false) }
    PreferenceItem(
        label = "GitHub token",
        supportingText = if (GithubPrefs.token.isBlank()) "Not configured" else "Connected as ${GithubPrefs.username}",
        icon = Icons.Outlined.Key,
        onClick = { dialogOpen = true }
    )

    if (dialogOpen) {
        GitHubTokenTopSheet(
            onDismiss = { dialogOpen = false },
            onSaved = { dialogOpen = false }
        )
    }
}

@Composable
internal fun GitHubTokenTopSheet(onDismiss: () -> Unit, onSaved: () -> Unit) {
    var sheetVisible by remember { mutableStateOf(false) }
    var showTokenHelp by rememberSaveable { mutableStateOf(false) }
    val fieldFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        sheetVisible = true
        delay(120)
        fieldFocusRequester.requestFocus()
        keyboard?.show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.42f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            AnimatedVisibility(
                visible = sheetVisible,
                enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
                    .statusBarsPadding().navigationBarsPadding().imePadding()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("GitHub token", style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                keyboard?.hide()
                                showTokenHelp = true
                            }) {
                                Icon(Icons.Outlined.Info, contentDescription = "Token permissions and help")
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Outlined.Close, contentDescription = "Close token editor")
                            }
                        }
                        GitHubTokenEntry(onSaved = onSaved, focusRequester = fieldFocusRequester)
                    }
                }
            }
        }
        if (showTokenHelp) {
            GitHubTokenAccessDialog(onDismiss = { showTokenHelp = false })
        }
    }
}

@Composable
private fun GitHubTokenEntry(onSaved: () -> Unit, focusRequester: FocusRequester) {
    var token by remember { mutableStateOf(GithubPrefs.token) }
    var verifying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = token,
            onValueChange = { token = it; error = null },
            label = { Text("Personal access token") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !verifying,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
        )
        error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(
                onClick = { uriHandler.openUri("https://github.com/settings/personal-access-tokens/new") },
                enabled = !verifying,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Create token") }
            Button(
                onClick = {
                    val candidate = token.trim()
                    error = null
                    if (candidate.isEmpty()) {
                        if (GithubPrefs.username.isNotBlank()) GithubPrefs.lastUsername = GithubPrefs.username
                        GithubPrefs.token = ""
                        GithubPrefs.username = ""
                        GithubPrefs.avatarUrl = ""
                        onSaved()
                        return@Button
                    }
                    verifying = true
                    scope.launch {
                        try {
                            val profile = GitHubDeviceAuth.accountProfile(candidate)
                            GithubPrefs.token = candidate
                            GithubPrefs.username = profile.login
                            GithubPrefs.lastUsername = profile.login
                            GithubPrefs.avatarUrl = profile.avatarUrl
                            GithubPrefs.guestMode = false
                            token = ""
                            onSaved()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            error = failure.message ?: "Could not verify this GitHub token"
                        } finally {
                            verifying = false
                        }
                    }
                },
                enabled = !verifying,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (verifying) CircularProgressIndicator(modifier = Modifier.size(18.dp).padding(end = 2.dp), strokeWidth = 2.dp)
                Text("Save token")
            }
        }
    }
}

/** A fixed-size dialog window lets Compose animate sections without resizing the Android window. */
@Composable
private fun GitHubTokenAccessDialog(onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var fineGrainedExpanded by rememberSaveable { mutableStateOf(false) }
    var classicExpanded by rememberSaveable { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.matchParentSize().clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss))
            Surface(
                modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth().clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceContainerHigh
            ) {
                Column {
                    Text("GitHub token help", modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.headlineSmall)
                    Column(
                        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Tap a token type to see its setup.", modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        TokenHelpSection(
                            title = "Fine-grained token",
                            expanded = fineGrainedExpanded,
                            onClick = { fineGrainedExpanded = !fineGrainedExpanded }
                        ) {
                            TokenHelpSetting("Resource owner", "Your GitHub account")
                            TokenHelpSetting("Repository access", "Public repositories (read-only)")
                            TokenHelpSetting("Additional permissions", "Leave unset")
                        }
                        TokenHelpSection(
                            title = "Classic token",
                            expanded = classicExpanded,
                            onClick = { classicExpanded = !classicExpanded }
                        ) {
                            TokenHelpSetting("Scopes", "Leave all checkboxes unchecked")
                            Text("No extra scopes are needed to read public NikGapps files.",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("NikGapps needs read-only access to public repositories to download packages. " +
                                "Using your token helps avoid download interruptions caused by GitHub's request limits.",
                                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            Text("Your private repositories and write access are not needed.",
                                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            Text("Tap Create token to open GitHub. Paste the generated token here, then tap Save token. " +
                                "To sign out, clear the field and save.",
                                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(color = colors.outlineVariant)
                    Button(onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(16.dp)) { Text("Got it") }
                }
            }
        }
    }
}

@Composable
private fun TokenHelpSection(
    title: String,
    expanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val chevronAngle by animateFloatAsState(if (expanded) 180f else 0f,
        animationSpec = tween(240, easing = FastOutSlowInEasing), label = "Token help chevron")
    val containerColor by animateColorAsState(if (expanded) colors.secondaryContainer else colors.surfaceContainer,
        animationSpec = tween(240), label = "Token help container")
    val borderColor by animateColorAsState(if (expanded) colors.primary else colors.outlineVariant,
        animationSpec = tween(240), label = "Token help border")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = if (expanded) colors.onSecondaryContainer else colors.onSurface,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                    .clickable(role = Role.Button, onClick = onClick)
                    .heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Icon(Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.rotate(chevronAngle))
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(240, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Top) + fadeIn(tween(180)),
                exit = shrinkVertically(animationSpec = tween(240, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top) + fadeOut(tween(180))
            ) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
            }
        }
    }
}

@Composable
private fun TokenHelpSetting(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

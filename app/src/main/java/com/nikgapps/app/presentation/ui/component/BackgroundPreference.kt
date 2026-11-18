package com.nikgapps.app.presentation.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nikgapps.app.data.BackgroundPrefs
import com.nikgapps.app.data.DisplayPrefs
import com.nikgapps.app.data.GlassBackground
import com.nikgapps.app.presentation.theme.withGlassAccent
import com.nikgapps.app.presentation.ui.component.dialogs.AccountDialog
import com.nikgapps.app.presentation.ui.component.items.PreferenceItem

@Composable
internal fun BackgroundPreference() {
    var open by rememberSaveable { mutableStateOf(false) }
    var info by rememberSaveable { mutableStateOf(false) }
    val enabled = DisplayPrefs.useLiquidGlass
    PreferenceItem(
        modifier = Modifier.clickable(enabled = enabled) { open = true },
        leadingContent = { Icon(Icons.Outlined.Wallpaper, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Background", style = MaterialTheme.typography.bodyLarge)
                IconButton(onClick = { info = true }) {
                    Icon(AppIcons.Info, "About backgrounds", Modifier.size(20.dp))
                }
            }
        },
        supportingText = { Text(if (enabled) BackgroundPrefs.selected.label else "Enable Liquid glass to choose",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { Icon(Icons.Outlined.ChevronRight, null,
            Modifier.alpha(if (enabled) 1f else .38f)) }
    )
    if (info) AccountDialog(title = "Backgrounds", onDismiss = { info = false },
        content = { Text("Choose a background for Liquid glass. Each option has matching button, checkbox and radio-button colors, and adapts to your light or dark theme. Preview a choice before applying it. Your choice is remembered when you reopen the app.",
            style = MaterialTheme.typography.bodyMedium) },
        actions = { GlassButton(onClick = { info = false }, modifier = Modifier.fillMaxWidth()) { Text("Got it") } })
    if (open) BackgroundPicker(onDismiss = { open = false })
}

@Composable
private fun BackgroundPicker(onDismiss: () -> Unit) {
    val current = BackgroundPrefs.selected
    var draftId by rememberSaveable(current.id) { mutableStateOf(current.id) }
    val draft = GlassBackground.fromId(draftId)
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val maxHeight = with(LocalDensity.current) { windowHeight.toDp() * .85f }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GlassSurface(Modifier.widthIn(max = 440.dp).fillMaxWidth(.94f).heightIn(max = maxHeight),
            shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column {
                Text("Background", Modifier.padding(20.dp), style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BackgroundPreview(draft)
                    GlassBackground.entries.chunked(2).forEach { options ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.forEach { option ->
                                BackgroundTile(option, option == draft, { draftId = option.id }, Modifier.weight(1f))
                            }
                            if (options.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTonalButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    GlassButton(onClick = { BackgroundPrefs.selected = draft; onDismiss() },
                        modifier = Modifier.weight(1f)) { Text("Apply") }
                }
            }
        }
    }
}

@Composable
private fun BackgroundTile(background: GlassBackground, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(14.dp)
    GlassSurface(modifier.clip(shape).selectable(selected, role = Role.RadioButton, onClick = onClick)
        .then(if (selected) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape) else Modifier),
        shape = shape, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            BackgroundSwatch(background, Modifier.fillMaxWidth().height(62.dp))
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(background.label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                RadioButton(selected, onClick = null, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun BackgroundPreview(background: GlassBackground) {
    val scheme = MaterialTheme.colorScheme
    MaterialTheme(colorScheme = scheme.withGlassAccent(scheme.background.luminance() < .5f, background)) {
        ProjectsBackdrop(modifier = Modifier.fillMaxWidth().height(128.dp).clip(RoundedCornerShape(16.dp)),
            background = background) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Preview", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FolderSpecial, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Your projects", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

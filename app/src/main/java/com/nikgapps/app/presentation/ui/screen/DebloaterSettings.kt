package com.nikgapps.app.presentation.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.nikgapps.app.registry.DebloaterConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebloaterSettings(projectId: String, projectName: String, targets: List<String>,
    conflicts: Set<String>, visible: Boolean, onDismiss: () -> Unit, onApply: (List<String>) -> Unit) {
    var entries by rememberSaveable(projectId) { mutableStateOf(emptyList<String>()) }
    var input by rememberSaveable(projectId) { mutableStateOf("") }
    var error by remember(projectId) { mutableStateOf<String?>(null) }
    var inputFocused by remember { mutableStateOf(false) }
    var wasVisible by rememberSaveable(projectId) { mutableStateOf(false) }
    var noticeType by rememberSaveable(projectId) { mutableStateOf("info") }
    var noticeVisible by rememberSaveable(projectId) { mutableStateOf(false) }
    val validation = remember(entries, conflicts) { runCatching { DebloaterConfig.validate(entries, conflicts) } }
    LaunchedEffect(visible) {
        if (visible && !wasVisible) { entries = targets; input = ""; error = null; noticeVisible = false }
        wasVisible = visible
    }
    fun addEntry() {
        runCatching { DebloaterConfig.add(entries, input, conflicts) }
            .onSuccess { entries = it; input = ""; error = null }
            .onFailure { error = it.message }
    }
    fun toggleNotice(type: String) {
        if (noticeVisible && noticeType == type) noticeVisible = false
        else { noticeType = type; noticeVisible = true }
    }
    if (visible) ModalBottomSheet(
        onDismissRequest = {}, // Drafts close only through Cancel or Apply.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden }),
        sheetGesturesEnabled = false,
        dragHandle = null,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = false,
            shouldDismissOnClickOutside = false)) {
        // Wrap short lists and grow upward from the bottom-anchored composer.
        // Long lists scroll within the available height rather than filling
        // short lists with blank space or pushing the composer off screen.
        val windowHeight = LocalWindowInfo.current.containerSize.height
        val viewportHeight = with(LocalDensity.current) { windowHeight.toDp() * 0.85f }
        Column(Modifier.fillMaxWidth().heightIn(max = viewportHeight)
            .padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Keep the notice in the header's layout slot. Its spacing must
            // collapse with it rather than disappearing after the animation.
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Configure debloater", style = MaterialTheme.typography.titleLarge)
                        Text("Project: $projectName", style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = { toggleNotice("info") }) {
                        Icon(Icons.Default.Info, if (noticeVisible && noticeType == "info") "Hide debloater information" else "Show debloater information",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { toggleNotice("warning") }) {
                        Icon(Icons.Default.Warning, if (noticeVisible && noticeType == "warning") "Hide debloater warning" else "Show debloater warning",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
                AnimatedVisibility(visible = noticeVisible,
                    enter = expandVertically(animationSpec = tween(180), expandFrom = Alignment.Top) + fadeIn(tween(120)),
                    exit = shrinkVertically(animationSpec = tween(180), shrinkTowards = Alignment.Top) + fadeOut(tween(100))) {
                    val warning = noticeType == "warning"
                    Surface(modifier = Modifier.padding(top = 12.dp),
                        color = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)) {
                        Text(if (warning) "These files and folders will be deleted when you flash the ZIP. This may stop Android from working. Removing an entry later will not restore them."
                            else "Tap + to add an app folder or a supported file path. Names are case sensitive, so youtube and YouTube are different. Wildcards, shell commands and known critical components are blocked.",
                            Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall,
                            color = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
            Surface(modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    val message = error ?: validation.exceptionOrNull()?.message
                    if (message != null) Text(message, Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    if (entries.isEmpty()) Text("No removal targets added. Nothing will be debloated.", Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    entries.forEachIndexed { index, entry ->
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(entry, Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 10.dp), style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { entries = entries.filterIndexed { position, _ -> position != index }; error = null }) {
                                Icon(Icons.Default.Close, "Remove $entry", Modifier.size(18.dp))
                            }
                        }
                        if (index != entries.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(if (inputFocused) 2.dp else 1.dp,
                        if (error != null) MaterialTheme.colorScheme.error else if (inputFocused) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline)) {
                    BasicTextField(value = input, onValueChange = { input = it; error = null }, singleLine = true,
                        modifier = Modifier.fillMaxSize().onFocusChanged { inputFocused = it.isFocused }
                            .semantics { contentDescription = "File or folder" },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (input.isNotBlank()) addEntry() }),
                        decorationBox = { field ->
                            Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                                if (input.isEmpty()) Text("File or folder", style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                field()
                            }
                        })
                }
                FilledIconButton(onClick = ::addEntry, enabled = input.isNotBlank(),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Add, "Add debloater target")
                }
            }
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Cancel") }
                FilledTonalButton(onClick = { onApply(validation.getOrThrow()); onDismiss() },
                    enabled = input.isBlank() && validation.isSuccess,
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Apply") }
            }
        }
    }
}

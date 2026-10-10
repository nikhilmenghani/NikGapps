package com.nikgapps.app.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nikgapps.app.data.BuildCompressionPrefs
import com.nikgapps.app.data.GithubPrefs
import com.nikgapps.app.utils.network.EliteMembershipRepository
import com.nikgapps.app.presentation.ui.component.items.PreferenceItem
import com.nikgapps.app.presentation.ui.component.items.CheckableItem
import com.nikgapps.app.presentation.ui.component.dialogs.BottomSheetDialog
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildCompressionOptions(
    compressed: Boolean = BuildCompressionPrefs.compressed,
    level: Int = BuildCompressionPrefs.level,
    onCompressedChange: (Boolean) -> Unit = { BuildCompressionPrefs.compressed = it },
    onLevelChange: (Int) -> Unit = { BuildCompressionPrefs.level = it },
    roundedCards: Boolean = false
) {
    var elite by remember(GithubPrefs.username) { mutableStateOf(false) }
    var showLevels by remember { mutableStateOf(false) }
    LaunchedEffect(GithubPrefs.username) {
        try {
            elite = EliteMembershipRepository.isElite(GithubPrefs.username)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) { elite = false }
    }
    val rowModifier = if (roundedCards) Modifier.clip(RoundedCornerShape(18.dp)) else Modifier
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (roundedCards) 10.dp else 4.dp)) {
        PreferenceItem(
            modifier = rowModifier,
            label = "Compressed build",
            supportingText = if (compressed) "Smaller ZIP; takes longer to build"
                else "Faster build with standard ZIP size",
            icon = Icons.Outlined.Inventory2,
            switchState = compressed,
            onSwitchChange = onCompressedChange
        )
            PreferenceItem(
                modifier = rowModifier,
                label = "Compression level",
                supportingText = when {
                    !elite -> "Elite required · Balanced compression (level 6)"
                    !compressed -> "Enable compressed builds to choose a level"
                    else -> compressionLevelLabel(level.coerceIn(1, 9))
                },
                icon = Icons.Outlined.Tune,
                enabled = compressed && elite, eliteOption = true,
                onClick = { showLevels = true }
            )
    }
    if (showLevels && elite) BottomSheetDialog(onDismissRequest = { showLevels = false }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Text("Compression level", style = MaterialTheme.typography.titleLarge)
            Text("Higher levels can make a smaller ZIP, but take longer to build.",
                Modifier.padding(top = 8.dp, bottom = 16.dp), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            (1..9).forEach { choice ->
                CheckableItem(text = compressionLevelLabel(choice),
                    isChecked = choice == level, icon = null) {
                    onLevelChange(choice)
                    showLevels = false
                }
            }
        }
    }
}

private fun compressionLevelLabel(level: Int): String = when (level) {
    1 -> "Level 1 · Fastest"
    6 -> "Level 6 · Balanced"
    9 -> "Level 9 · Maximum compression"
    else -> "Level $level"
}

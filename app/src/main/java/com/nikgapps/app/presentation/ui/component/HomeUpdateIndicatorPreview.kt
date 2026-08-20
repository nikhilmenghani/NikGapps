package com.nikgapps.app.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikgapps.app.presentation.theme.NikGappsThemePreview

@Preview(name = "Home update indicator states", showBackground = true, widthDp = 412)
@Composable
private fun HomeUpdateIndicatorPreview() {
    NikGappsThemePreview {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PreviewTopBar(isDownloading = false)
                PreviewTopBar(isDownloading = true)
            }
        }
    }
}

@Composable
private fun PreviewTopBar(isDownloading: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("NikGapps", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(4.dp))
        Text(
            "v0.80.11",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        HomeUpdateIndicator(
            latestVersion = "0.80.12",
            isDownloading = isDownloading,
            onClick = {}
        )
        IconButton(onClick = {}) { Icon(Icons.Default.Refresh, "Restart") }
        IconButton(onClick = {}) { Icon(Icons.Default.Settings, "Settings") }
    }
}

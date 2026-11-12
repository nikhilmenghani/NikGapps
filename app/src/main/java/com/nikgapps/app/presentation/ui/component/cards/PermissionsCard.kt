package com.nikgapps.app.presentation.ui.component.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikgapps.app.presentation.theme.NikGappsThemePreview

@Composable
fun PermissionsCard(
    title: String = "Notifications",
    description: String = "See build progress and completion updates.",
    isPermissionGranted: Boolean = false,
    onRequestPermission: () -> Unit,
    permissionsText: String = "Allow access",
    icon: ImageVector = Icons.Outlined.Shield
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainer,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(14.dp), color = colors.surfaceContainerHighest) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Icon(icon, null, Modifier.size(24.dp), tint = colors.onSurfaceVariant)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(if (isPermissionGranted) "Allowed" else "Not allowed",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isPermissionGranted) colors.primary else colors.onSurfaceVariant)
                }
                if (isPermissionGranted) Icon(Icons.Default.CheckCircle, "Access allowed",
                    tint = colors.primary, modifier = Modifier.size(22.dp))
            }
            Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            if (!isPermissionGranted) {
                HorizontalDivider(color = colors.outlineVariant)
                FilledTonalButton(onClick = onRequestPermission, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(permissionsText)
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewSamplePermissionsManagerCard() {
    NikGappsThemePreview {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PermissionsCard(onRequestPermission = {})
            PermissionsCard(isPermissionGranted = true, onRequestPermission = {})
        }
    }
}

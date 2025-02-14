package com.nikgapps.app.presentation.ui.component.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikgapps.app.presentation.theme.NikGappsThemePreview
import com.nikgapps.app.presentation.ui.component.common.StyledText

@Composable
fun PermissionsItem(
    title: String,
    description: String,
    isOptional: Boolean,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StyledText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1
                )

                if (!isOptional) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Required",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            StyledText(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (!isGranted) {
            if (!isOptional) {
                Button(
                    onClick = onGrantClick,
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    StyledText(
                        text = "Grant",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                TextButton(onClick = onGrantClick) {
                    StyledText(text = "Grant")
                }
            }
        } else {
            StyledText(
                text = "Granted",
                modifier = Modifier.padding(ButtonDefaults.TextButtonContentPadding),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.primary.copy(0.7f)
                )
            )
        }
    }
}

@Preview
@Composable
fun StartSettingsLayoutPermissionsItemPreview() {
    NikGappsThemePreview {
        PermissionsItem(
            title = "Title",
            description = "Description",
            isOptional = false,
            isGranted = true,
            onGrantClick = {}
        )
    }
}
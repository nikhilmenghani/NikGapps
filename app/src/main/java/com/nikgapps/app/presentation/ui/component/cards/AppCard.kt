package com.nikgapps.app.presentation.ui.component.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikgapps.app.data.model.InstalledAppInfo

@Composable
fun AppCard(appInfo: InstalledAppInfo, elevation: Dp) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(elevation),
        colors = CardDefaults.cardColors(
            containerColor = when (appInfo.appType) {
                "Updated System App" -> MaterialTheme.colorScheme.secondary
                "System App" -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.background
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = appInfo.appIcon as Painter,
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 16.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 18.sp
                )
            }
            Text(
                text = if (appInfo.isSystemApp) "System App" else "User App",
                modifier = Modifier.padding(4.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAppCard() {
    val sampleAppInfo = InstalledAppInfo(
        appName = "Sample App",
        packageName = "com.example.sample",
        installLocation = "/data/app/com.example.sample",
        appIcon = ColorPainter(Color.Gray),
        isSystemApp = false,
        appType = "User App"
    )
    AppCard(appInfo = sampleAppInfo, elevation = 16.dp)
}

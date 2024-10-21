package com.nikgapps.ui.components.cards

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
import com.nikgapps.ui.model.InstalledAppInfo

@Composable
fun AppCard(appInfo: InstalledAppInfo, elevation: Dp) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(elevation),
        colors = CardDefaults.cardColors(containerColor = if (appInfo.isSystemApp) Color.LightGray else Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = appInfo.appIcon as Painter,
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 16.dp)
            )
            Column() {
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 18.sp
                )
                Text(
                    text = "Install Location: ${appInfo.installLocation}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (appInfo.isSystemApp) "Type: System App" else "Type: User App",
                    style = MaterialTheme.typography.bodySmall
                )
            }
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
        isSystemApp = false
    )
    AppCard(appInfo = sampleAppInfo, elevation = 16.dp)
}
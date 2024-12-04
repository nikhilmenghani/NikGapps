package com.nikgapps.app.presentation.ui.component.cards

import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaredrummler.android.device.DeviceName
import com.nikgapps.App
import com.nikgapps.app.presentation.theme.NikGappsThemePreview
import com.nikgapps.app.utils.deviceinfo.getActiveSlot
import com.nikgapps.app.utils.deviceinfo.hasDynamicPartitions
import com.nikgapps.app.utils.deviceinfo.isABDevice
import com.nikgapps.dumps.RootUtility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DeviceStatsCard() {
    var isABDevice by remember { mutableStateOf(false) }
    var activeSlot by remember { mutableStateOf("unknown") }
    var hasDynamicPartitions by remember { mutableStateOf(false) }
    var deviceName by remember { mutableStateOf(Build.MODEL) }
    var deviceCode by remember { mutableStateOf(Build.DEVICE) }
    val currentVersion = Build.VERSION.RELEASE
    var rootAccessState by remember { mutableStateOf(App.hasRootAccess) }

    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            isABDevice = isABDevice()
            activeSlot = getActiveSlot()
            hasDynamicPartitions = hasDynamicPartitions()
            deviceName = DeviceName.getDeviceName(deviceCode, Build.MODEL)
            if (deviceName == deviceCode) {
                deviceName = Build.MODEL
            }
            // Check root access
            val rootAccess = RootUtility.hasRootAccess()
            Log.d("NikGapps-RootAccess", "Root Access: $rootAccess")
            App.hasRootAccess = rootAccess

            withContext(Dispatchers.Main) {
                rootAccessState = rootAccess
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = "Device Information",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Android Version
            DeviceInfoRow(
                label = "Android Version",
                value = currentVersion,
                icon = Icons.Default.Android,
                labelColor = MaterialTheme.colorScheme.onSurface,
                iconColor = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )

            // Device Name Row
            DeviceInfoRow(
                icon = Icons.Default.Memory,
                label = "Device Name",
                value = "$deviceName | $deviceCode",
                labelColor = MaterialTheme.colorScheme.onSurface,
                iconColor = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )

            // Partition Scheme
            DeviceInfoRow(
                label = "Partition Scheme",
                value = if (isABDevice) "A/B Partitions" else "Non-A/B Partitions",
                icon = Icons.Default.SwapHoriz,
                labelColor = MaterialTheme.colorScheme.onSurface,
                iconColor = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )

            // Active Slot
            if (activeSlot.isNotEmpty()) {
                DeviceInfoRow(
                    label = "Active Slot",
                    value = activeSlot,
                    icon = Icons.Default.Memory,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconColor = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            }

            // Dynamic Partitions
            DeviceInfoRow(
                label = "Dynamic Partitions",
                value = if (hasDynamicPartitions) "Supported" else "Not Supported",
                icon = Icons.Default.DynamicFeed,
                labelColor = MaterialTheme.colorScheme.onSurface,
                iconColor = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )

            // Root Access
            DeviceInfoRow(
                label = "Root Access",
                value = if (rootAccessState) "Granted" else "Not Granted",
                icon = if (rootAccessState) Icons.Default.CheckCircle else Icons.Default.Cancel,
                valueColor = if (rootAccessState) Color(0xFF388E3C) else Color(0xFFD32F2F),
                labelColor = MaterialTheme.colorScheme.onSurface,
                iconColor = if (rootAccessState) Color(0xFF388E3C) else Color(0xFFD32F2F)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )

            // Refresh Button
            Button(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        val rootAccess = RootUtility.hasRootAccess()
                        Log.d("NikGapps-RootAccess", "Root Access: $rootAccess")
                        App.hasRootAccess = rootAccess

                        withContext(Dispatchers.Main) {
                            rootAccessState = rootAccess
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = "Refresh Root Access")
            }
        }
    }
}

@Composable
fun DeviceInfoRow(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = valueColor
            )
        }
    }
}

@Preview(name = "Dark Theme", showBackground = true)
@Composable
fun DeviceStatCardPreview() {
    NikGappsThemePreview {
        DeviceStatsCard()
    }
}
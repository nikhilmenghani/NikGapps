package com.nikgapps.app.presentation.ui.component.layouts

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaredrummler.android.device.DeviceName
import com.nikgapps.App
import com.nikgapps.app.presentation.theme.NikGappsThemePreview
import com.nikgapps.app.presentation.ui.component.cards.DeviceInfoRow
import com.nikgapps.app.utils.deviceinfo.getActiveSlot
import com.nikgapps.app.utils.deviceinfo.hasDynamicPartitions
import com.nikgapps.app.utils.deviceinfo.isABDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceStats() {
    var isABDevice by remember { mutableStateOf(false) }
    var activeSlot by remember { mutableStateOf("unknown") }
    var hasDynamicPartitions by remember { mutableStateOf(false) }
    var deviceName by remember { mutableStateOf(Build.MODEL) }
    var deviceCode by remember { mutableStateOf(Build.DEVICE) }
    val currentVersion = Build.VERSION.RELEASE

    LaunchedEffect(Unit) {
        isABDevice = isABDevice()
        activeSlot = getActiveSlot()
        hasDynamicPartitions = hasDynamicPartitions()
        DeviceName.with(App.globalClass).request { info, error ->
            if (error == null) {
                deviceName = info.marketName ?: Build.MODEL
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
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
                color = Color(0xFF6200EA),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Android Version Row
            DeviceInfoRow(
                icon = Icons.Default.Android,
                label = "Android Version",
                value = currentVersion
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Device Name Row
            DeviceInfoRow(
                icon = Icons.Default.Memory,
                label = "Device Name",
                value = "$deviceName | $deviceCode"
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Partition Scheme Row
            DeviceInfoRow(
                icon = Icons.Default.SwapHoriz,
                label = "Partition Scheme",
                value = if (isABDevice) "A/B Partitions" else "Non-A/B Partitions"
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Active Slot Row
            if (activeSlot.isNotEmpty()) {
                DeviceInfoRow(
                    icon = Icons.Default.Memory,
                    label = "Active Slot",
                    value = activeSlot
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Dynamic Partitions Row
            DeviceInfoRow(
                icon = Icons.Default.DynamicFeed,
                label = "Dynamic Partitions",
                value = if (hasDynamicPartitions) "Supported" else "Not Supported"
            )
        }
    }
}

@Preview(name = "Dark Theme", showBackground = true, backgroundColor = 0xFF000000 )
@Composable
fun DeviceStatsPreview() {
    NikGappsThemePreview {
        DeviceStats()
    }
}


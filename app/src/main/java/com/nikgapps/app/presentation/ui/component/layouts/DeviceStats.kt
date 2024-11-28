package com.nikgapps.app.presentation.ui.component.layouts

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.unit.dp
import com.nikgapps.app.presentation.ui.component.cards.DeviceStatCard
import com.nikgapps.app.utils.deviceinfo.getActiveSlot
import com.nikgapps.app.utils.deviceinfo.hasDynamicPartitions
import com.nikgapps.app.utils.deviceinfo.isABDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceStats() {
    var isABDevice by remember { mutableStateOf(false) }
    var activeSlot by remember { mutableStateOf("unknown") }
    var hasDynamicPartitions by remember { mutableStateOf(false) }
    val currentVersion = Build.VERSION.RELEASE

    LaunchedEffect(Unit) {
        isABDevice = isABDevice()
        activeSlot = getActiveSlot()
        hasDynamicPartitions = hasDynamicPartitions()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Device Information",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Android Version Card
        DeviceStatCard(
            icon = Icons.Default.Android,
            title = "Android Version",
            value = currentVersion
        )

        Spacer(modifier = Modifier.height(16.dp))

        // A/B Partition Card
        DeviceStatCard(
            icon = Icons.Default.SwapHoriz,
            title = "Partition Scheme",
            value = if (isABDevice) "A/B Partitions" else "Non-A/B Partitions"
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (activeSlot != "") {
            // Active Slot Card
            DeviceStatCard(
                icon = Icons.Default.Memory,
                title = "Active Slot",
                value = activeSlot
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Dynamic Partitions Card
        DeviceStatCard(
            icon = Icons.Default.DynamicFeed,
            title = "Dynamic Partitions",
            value = if (hasDynamicPartitions) "Supported" else "Not Supported"
        )
    }
}


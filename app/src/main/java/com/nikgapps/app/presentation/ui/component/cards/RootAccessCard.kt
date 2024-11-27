package com.nikgapps.app.presentation.ui.component.cards

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikgapps.App
import com.nikgapps.app.presentation.ui.component.buttons.FilledTonalButtonWithIcon
import com.nikgapps.dumps.RootUtility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RootAccessCard() {
    var rootAccessState by remember { mutableStateOf(App.hasRootAccess) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rootAccessState) Color(0xFFB9F6CA) // Light Green for Granted
            else Color(0xFFF36170) // Light Red for Denied
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButtonWithIcon(
                icon = Icons.Default.LockOpen, text = "Get Root Access",
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        // Always perform a fresh check for root access
                        val rootAccess = RootUtility.hasRootAccess()
                        Log.d("NikGapps-RootAccess", "Root Access: $rootAccess")
                        App.hasRootAccess = rootAccess

                        // Update the state variable to trigger recomposition
                        withContext(Dispatchers.Main) {
                            rootAccessState = rootAccess
                        }
                    }
                })
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Access ${if (rootAccessState) "Granted" else "Not Granted"}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (rootAccessState) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError
            )
        }
    }
}

@Preview(name = "phone", device = "spec:width=360dp,height=640dp,dpi=480")
@Preview(name = "landscape", device = "spec:width=640dp,height=360dp,dpi=480")
@Preview(name = "foldable", device = "spec:width=673dp,height=841dp,dpi=480")
@Preview(name = "tablet", device = "spec:width=1280dp,height=800dp,dpi=480")
@Composable
fun PreviewGetRootAccessCard() {
    RootAccessCard()
}

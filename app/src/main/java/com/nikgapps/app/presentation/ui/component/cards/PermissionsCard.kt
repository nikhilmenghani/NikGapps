package com.nikgapps.app.presentation.ui.component.cards


import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nikgapps.app.utils.constants.permissionMap
import com.nikgapps.app.utils.permissions.Permissions
import com.nikgapps.app.utils.settings.Settings

@Composable
fun PermissionsCard(
    isPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    permissionsText: String = "Permission"
) {
    val backgroundColor = if (isPermissionGranted) Color(0xFFB9F6CA) else Color(0xFFF36170)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Button to request permission
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(permissionsText)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun PermissionsManager() {
    Column {
        permissionMap.forEach { (permissionName, _) ->
            PermissionsManagerCard(permissionName = permissionName)
        }
    }
}

@SuppressLint("InlinedApi")
@Composable
fun PermissionsManagerCard(permissionName: String = "Notifications") {
    val context = LocalContext.current
    // Track permission state and message text
    var hasPermission by remember { mutableStateOf(Permissions.isPermissionGranted(context, permissionName)) }
    var permanentlyDenied by remember { mutableStateOf(Permissions.isPermissionPermanentlyDenied(context, permissionName)) }
    var permissionsText by remember {
        mutableStateOf(
            if (hasPermission) "$permissionName Permission Granted" else "Request $permissionName Permission"
        )
    }

    // Use Permissions object to handle permission request
    val requestPermissionLauncher = Permissions.requestPermission(
        context = context,
        permissionName = permissionName
    ) { isGranted, isPermanentlyDenied ->
        hasPermission = isGranted
        permanentlyDenied = isPermanentlyDenied
        permissionsText = when {
            isGranted -> "$permissionName Permission Granted"
            isPermanentlyDenied -> "Denied Permanently, Go to Settings"
            else -> "$permissionName Permission Denied"
        }
    }

    // Observe lifecycle changes to recheck permission on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Re-check permission when returning to the app
                hasPermission = Permissions.isPermissionGranted(context, permissionName)
                permanentlyDenied = Permissions.isPermissionPermanentlyDenied(context, permissionName)
                permissionsText = when {
                    hasPermission -> "$permissionName Permission Granted"
                    permanentlyDenied -> "Denied Permanently, Go to Settings"
                    else -> "Request $permissionName Permission"
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    PermissionsCard(
        isPermissionGranted = hasPermission,
        permissionsText = permissionsText,
        onRequestPermission = {
            if (!hasPermission) {
                if (permanentlyDenied) {
                    Settings.openAppSettings(context)
                } else {
                    requestPermissionLauncher?.launch(permissionMap[permissionName]?.permission ?: "")
                }
            } else {
                Toast.makeText(context, "Permission already granted", Toast.LENGTH_SHORT).show()
            }
        }
    )
}


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview
@Composable
fun PreviewSamplePermissionsManagerCard() {
    PermissionsManager()
}

package com.nikgapps.app.presentation.ui.component.cards

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
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
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nikgapps.app.utils.permissions.Notifications
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

@SuppressLint("InlinedApi")
@Composable
fun PermissionsManagerCard() {
    val context = LocalContext.current

    // Track permission state
    var hasPermission by remember { mutableStateOf(Notifications.checkPermission(context)) }
    var permissionsText by remember { mutableStateOf(if (hasPermission) "Notifications Permission Granted" else "Request Notifications Permission") }

    // Track if permission is permanently denied (when "Don't ask again" is selected)
    var permanentlyDenied by remember {
        mutableStateOf(
            !hasPermission && !ActivityCompat.shouldShowRequestPermissionRationale(
                context as Activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )
    }

    // Permission request launcher with callback
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            permanentlyDenied =
                !isGranted && !ActivityCompat.shouldShowRequestPermissionRationale(
                    context as Activity,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            // Update permissions text
            permissionsText = if (isGranted) {
                "Notifications Permission Granted"
            } else if (permanentlyDenied) {
                "Denied Permanently, Go to Settings"
            } else {
                "Notifications Permission Denied"
            }
            Toast.makeText(context, permissionsText, Toast.LENGTH_SHORT).show()
        }
    )

    // Observe lifecycle changes to recheck permission on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Re-check permission when returning to the app
                hasPermission = Notifications.checkPermission(context)
                // Re-evaluate if permanently denied
                permanentlyDenied = !hasPermission && !ActivityCompat.shouldShowRequestPermissionRationale(
                    context as Activity,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                permissionsText = if (hasPermission) {
                    "Notifications Permission Granted"
                } else if (permanentlyDenied) {
                    "Denied Permanently, Go to Settings"
                } else {
                    "Request Notifications Permission"
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
                    Settings.openNotificationSettings(context)
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
    PermissionsManagerCard()
}

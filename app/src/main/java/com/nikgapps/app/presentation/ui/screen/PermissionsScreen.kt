package com.nikgapps.app.presentation.ui.screen

import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nikgapps.app.presentation.navigation.Screens
import com.nikgapps.app.presentation.ui.component.bottomsheets.ProfileBottomSheet
import com.nikgapps.app.presentation.ui.component.dialogs.BottomSheetDialog
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.nikgapps.app.presentation.theme.NikGappsTheme
import com.nikgapps.app.presentation.ui.component.cards.PermissionsCard
import com.nikgapps.app.utils.constants.permissionMap
import com.nikgapps.app.utils.extensions.navigateWithState
import com.nikgapps.app.utils.permissions.Permissions
import com.nikgapps.app.utils.settings.Settings
import kotlinx.coroutines.launch
import kotlin.collections.component1
import kotlin.collections.component2

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(onAllPermissionsGranted: () -> Unit = {}) {
    val context = LocalContext.current
    var allPermissionsGranted by remember {
        mutableStateOf(Permissions.hasAllRequiredPermissions(context))
    }
    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            onAllPermissionsGranted() // Automatically navigate to home when all permissions are granted
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Permissions Screen") }) }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                permissionMap.forEach { (permissionName, _) ->
                    PermissionsManagerCard(
                        permissionName = permissionName,
                        onPermissionStatusChanged = { isGranted ->
                            allPermissionsGranted = isGranted && Permissions.hasAllRequiredPermissions(context)
                        })
                }
            }
        }
    }
}

@SuppressLint("InlinedApi")
@Composable
fun PermissionsManagerCard(
    permissionName: String = "Notifications",
    onPermissionStatusChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            Permissions.isPermissionGranted(
                context,
                permissionName
            )
        )
    }
    var permanentlyDenied by remember {
        mutableStateOf(
            Permissions.isPermissionPermanentlyDenied(
                context,
                permissionName
            )
        )
    }
    var permissionsText by remember {
        mutableStateOf(
            if (hasPermission) "$permissionName Permission Granted" else "Request $permissionName Permission"
        )
    }

    val requestPermissionLauncher = Permissions.requestPermission(
        context = context,
        permissionName = permissionName
    ) { isGranted, isPermanentlyDenied ->
        hasPermission = isGranted
        permanentlyDenied = isPermanentlyDenied
        onPermissionStatusChanged(isGranted)
        permissionsText = when {
            isGranted -> "$permissionName Permission Granted"
            isPermanentlyDenied -> "Request $permissionName Permission, Go to Settings"
            else -> "$permissionName Permission Denied"
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = Permissions.isPermissionGranted(context, permissionName)
                permanentlyDenied =
                    Permissions.isPermissionPermanentlyDenied(context, permissionName)
                onPermissionStatusChanged(hasPermission)
                permissionsText = when {
                    hasPermission -> "$permissionName Permission Granted"
                    permanentlyDenied -> "Request $permissionName Permission, Go to Settings"
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
        title = "$permissionName Permission",
        description = permissionMap[permissionName]?.rationale
            ?: "We need this permission for better functionality.",
        isPermissionGranted = hasPermission,
        permissionsText = permissionsText,
        onRequestPermission = {
            if (!hasPermission) {
                when (permissionName) {
                    "Install Apps" -> {
                        Settings.openSettings(context, permissionMap[permissionName]?.action ?: "")
                    }

                    "Storage" -> {
                        Settings.openSettings(context, permissionMap[permissionName]?.action ?: "")
                    }

                    else -> {
                        if (permanentlyDenied) {
                            Settings.openSettings(context, permissionMap[permissionName]?.action ?: "")
                        } else {
                            val permissions =
                                permissionMap[permissionName]?.permission ?: arrayOf("")
                            permissions.forEach { permission ->
                                // Mark the permission as requested
                                Permissions.markPermissionAsRequested(context, permissionName)
                                requestPermissionLauncher?.launch(permission)
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Permission already granted", Toast.LENGTH_SHORT).show()
            }
        }
    )
}


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview(name = "Light Theme", showBackground = true)
@Composable
fun PreviewLightPermissionsScreen() {
    val navController = rememberNavController()
    MaterialTheme(
        colorScheme = lightColorScheme()
    ) {
        PermissionsScreen()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview(name = "Dark Theme", showBackground = true)
@Composable
fun PreviewDarkPermissionsScreen() {
    val navController = rememberNavController()
    NikGappsTheme {
        PermissionsScreen()
    }
}





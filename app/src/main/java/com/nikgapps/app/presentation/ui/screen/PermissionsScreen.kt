package com.nikgapps.app.presentation.ui.screen

import android.annotation.SuppressLint
import android.os.Build
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.nikgapps.app.presentation.theme.NikGappsTheme
import com.nikgapps.app.presentation.ui.component.cards.PermissionsManager
import com.nikgapps.app.utils.extensions.navigateWithState
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(navController: NavHostController) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var isSheetVisible by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Permissions Screen") }) }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isSheetVisible) {
                BottomSheetDialog(
                    onDismissRequest = { isSheetVisible = false },
                    sheetState = sheetState
                ) {
                    ProfileBottomSheet(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                isSheetVisible = false
                            }
                        }
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { isSheetVisible = true }) {
                    Text("Show Bottom Sheet")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.navigateWithState(route = Screens.Home.name) }) {
                    Text("Take me Home")
                }
                PermissionsManager()
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview(name = "Light Theme", showBackground = true)
@Composable
fun PreviewLightPermissionsScreen() {
    val navController = rememberNavController()
    MaterialTheme(
        colorScheme = lightColorScheme()
    ) {
        PermissionsScreen(navController = navController)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview(name = "Dark Theme", showBackground = true)
@Composable
fun PreviewDarkPermissionsScreen() {
    val navController = rememberNavController()
    NikGappsTheme {
        PermissionsScreen(navController = navController)
    }
}





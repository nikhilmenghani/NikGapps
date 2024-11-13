package com.nikgapps.app.presentation.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikgapps.app.presentation.ui.component.dialogs.BottomSheetDialog
import com.nikgapps.app.utils.extensions.Space
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var isSheetVisible by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile Screen") }) }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isSheetVisible){
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
            Button(onClick = { isSheetVisible = true }) {
                Text("Show Bottom Sheet")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileBottomSheet(
    onClick: () -> Unit
){
    Text(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        text = "This is a Modal Bottom Sheet",
        fontFamily = FontFamily.Serif,
        fontSize = 22.sp
    )
    Space(size = 8.dp)
    Button(onClick) {
        Text("Dismiss")
    }
    Space(size = 16.dp)
}

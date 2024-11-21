package com.nikgapps.app.presentation.ui.component.bottomsheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikgapps.app.data.model.LogManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallZipProgressBottomSheet(onDismiss: () -> Unit) {
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden } // Prevent dismissing by swipe
    )
    val scope = rememberCoroutineScope()
    val logs by remember { derivedStateOf { LogManager.logs } }
    val scrollState = rememberScrollState()

    // Automatically scroll to the bottom when new logs are added
    LaunchedEffect(logs.size) {
        scope.launch {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val maxHeight = screenHeight * 0.6f // Allow the height to grow up to 60% of the screen height

    ModalBottomSheet(
        onDismissRequest = {}, // Prevent dismissing by clicking outside
        sheetState = bottomSheetState
    ) {
        Box(
            modifier = Modifier
                .wrapContentHeight() // Initially wrap the content height
                .heightIn(max = maxHeight)
                .background(Color.Black)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = {
                        scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                            onDismiss()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Bottom Sheet", tint = Color.White)
                    }
                }
                logs.forEach { log ->
                    Text(
                        text = log,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

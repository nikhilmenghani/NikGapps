package com.nikgapps.app.presentation.ui.component.bottomsheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallZipProgressBottomSheet(
    viewModel: ProgressLogViewModel,
    onDismiss: () -> Unit,
    isProcessing: Boolean
) {
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden } // Prevent dismissing by swipe
    )
    val scope = rememberCoroutineScope()
    val progressLogs by viewModel.progressLogs.collectAsState()
    val scrollState = rememberScrollState()

    // Automatically scroll to the bottom when new logs are added
    LaunchedEffect(progressLogs.size) {
        scope.launch {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val minHeight = screenHeight * 0.4f // Allow the height to shrink down to 40% of the screen height
    val maxHeight = screenHeight * 0.6f // Allow the height to grow up to 60% of the screen height

    ModalBottomSheet(
        onDismissRequest = {
            if (isProcessing) {
                scope.launch { bottomSheetState.show() }
            } else {
                scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                    viewModel.clearLogs()
                    onDismiss()
                }
            }
        },
        sheetState = bottomSheetState
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight() // Initially wrap the content height
                .heightIn(min = minHeight, max = maxHeight)
                .background(Color.Black)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                progressLogs.forEach { log ->
                    val textColor = when {
                        log.contains("Extracting zip file...", ignoreCase = true) -> Color.Green
                        log.contains("Installing NikGapps...", ignoreCase = true) -> Color.Green
                        log.contains("Successful!", ignoreCase = true) -> Color.Green
                        else -> Color.White
                    }

                    val fontSize = when {
                        log.contains("Extracting zip file...", ignoreCase = true) -> 16.sp
                        log.contains("Installing NikGapps...", ignoreCase = true) -> 16.sp
                        log.contains("Successful!", ignoreCase = true) -> 16.sp
                        else -> 14.sp
                    }

                    Text(
                        text = log,
                        color = textColor,
                        fontSize = fontSize
                    )
                }
            }
            if (!isProcessing) {
                FloatingActionButton(
                    onClick = {
                        scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                            viewModel.clearLogs()
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Bottom Sheet"
                    )
                }
            }
        }
    }
}

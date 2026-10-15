package com.nikgapps.dumps

import androidx.compose.runtime.Composable
import com.nikgapps.app.presentation.ui.component.cards.DownloadNikGappsCard

/** Legacy entry point shares the maintained release selector; it never flashes a download. */
@Composable
fun DownloadAndExtractButton() = DownloadNikGappsCard()

package com.nikgapps.ui.components.texts

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.nikgapps.App

@Composable
fun RootStatusDisplay() {
    val rootStatus = if (App.hasRootAccess) "Root Access Granted" else "No Root Access"
    Text(text = rootStatus)
}

@Preview(showBackground = true)
@Composable
fun PreviewRootStatusDisplay() {
    RootStatusDisplay()
}

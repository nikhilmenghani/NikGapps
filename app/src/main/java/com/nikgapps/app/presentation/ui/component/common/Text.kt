package com.nikgapps.app.presentation.ui.component.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.nikgapps.App
import com.nikgapps.app.presentation.theme.NikGappsTheme

@Composable
fun Greeting(name: String) {
    Text(text = name)
}

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


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    NikGappsTheme {
        Greeting("Hello World")
    }
}
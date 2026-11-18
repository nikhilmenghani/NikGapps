package com.nikgapps.app.presentation.ui.component.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.nikgapps.app.presentation.theme.NikGappsTheme

@Composable
fun Greeting(name: String) {
    Text(text = name)
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    NikGappsTheme {
        Greeting("Hello World")
    }
}

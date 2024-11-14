package com.nikgapps.app.presentation.ui.component.bottomsheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikgapps.app.utils.extensions.Space

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
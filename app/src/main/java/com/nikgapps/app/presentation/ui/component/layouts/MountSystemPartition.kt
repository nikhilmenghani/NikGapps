package com.nikgapps.app.presentation.ui.component.layouts

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikgapps.App
import com.nikgapps.R
import com.nikgapps.app.utils.root.RootManager

@Composable
fun MountSystemPartition() {
    var mountResult by remember { mutableStateOf<Boolean?>(null) }
    var resulttext by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Button(onClick = {
                var rootManager = RootManager(App.globalClass)
                var result = rootManager.executeScript(R.raw.mount)
                resulttext = result.output
                Log.d("MountSystemPartition", "Mount result: $result")
                mountResult = result.success
            }) {
                Text(text = "Execute mount.sh")
            }

            Button(onClick = {
                var rootManager = RootManager(App.globalClass)
                var result = rootManager.executeScript(R.raw.test)
                resulttext = result.output
                Log.d("MountSystemPartition", "Test result: $result")
                mountResult = result.success
            }) {
                Text(text = "Execute test.sh")
            }
        }

        mountResult?.let { result ->
            Text(
                text = resulttext,
                color = if (result) Color.Green else Color.Red,
                fontSize = 10.sp
            )
        }
    }
}
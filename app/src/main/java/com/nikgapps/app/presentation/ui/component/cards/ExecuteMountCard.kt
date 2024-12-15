package com.nikgapps.app.presentation.ui.component.cards

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repartition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nikgapps.R
import com.nikgapps.app.utils.constants.ApplicationConstants.NIKGAPPS_APP_DIR
import com.nikgapps.app.utils.managers.ResourceManager
import com.nikgapps.app.utils.managers.ScriptManager
import com.nikgapps.app.utils.root.RootManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ExecuteMountCard() {
    var isProcessing by rememberSaveable { mutableStateOf(false) }
    var mountResult by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var resultText by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    ActionCard(
        title = "Execute Mount Script",
        description = "Run the mount script to prepare partitions for modifications. Requires root access.",
        buttonText = "Execute Mount",
        icon = Icons.Default.Repartition,
        isProcessing = isProcessing,
        onClick = {
            isProcessing = true
            CoroutineScope(Dispatchers.IO).launch {
                val rootManager = RootManager(context)
                val resManager = ResourceManager(context)
                val scripts = listOf("mount", "test")
                for (script in scripts) {
                    ScriptManager.createScriptFile("$NIKGAPPS_APP_DIR/$script.sh", resManager.getScript(script))
                    val result = rootManager.executeScriptAsRoot("$NIKGAPPS_APP_DIR/$script.sh")
                    resultText += result.output
                    if (result.output == "Exception: Cannot run program \"su\": error=2, No such file or directory") {
                        resultText += "\nRoot access is required to execute the script"
                        resultText += "\nPlease make sure you have root access and permissions granted to the app"
                    }
                    Log.d("ExecuteMountCard", "Mount result: $result")
                    mountResult = result.success
                    if (!result.success) {
                        break
                    }
                }
                isProcessing = false
            }
        }
    )

    mountResult?.let { result ->
        Text(
            text = resultText,
            color = if (result) Color.Green else Color.Red,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(10.dp)
        )
    }
}

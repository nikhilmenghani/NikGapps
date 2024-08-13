import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikgapps.data.SettingItem
import com.nikgapps.data.SettingType
import com.nikgapps.ui.screens.ChooseAccountDialog

@Composable
fun SettingItemView(setting: SettingItem, onSettingChanged: (SettingItem) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        ChooseAccountDialog(
            accounts = listOf("Account 1", "Account 2", "Account 3"), // Replace with actual accounts
            selectedAccount = setting.value as String,
            onAccountSelected = { selectedAccount ->
                onSettingChanged(setting.copy(value = selectedAccount))
            },
            onDismiss = { showDialog = false }
        )
    }

    when (setting.type) {
        is SettingType.Toggle -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = setting.textToDisplay)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = setting.value as Boolean,
                    onCheckedChange = { isChecked ->
                        onSettingChanged(setting.copy(value = isChecked))
                    }
                )
            }
        }
        is SettingType.Text -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = setting.textToDisplay)
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = setting.value as String,
                    onValueChange = { value ->
                        onSettingChanged(setting.copy(value = value))
                    }
                )
            }
        }
        is SettingType.Checkbox -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = setting.value as Boolean,
                    onCheckedChange = { isChecked ->
                        onSettingChanged(setting.copy(value = isChecked))
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = setting.textToDisplay)
            }
        }
        is SettingType.Radio -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = setting.textToDisplay)
                Spacer(modifier = Modifier.height(8.dp))
                setting.type.options.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = setting.value == option,
                            onClick = {
                                onSettingChanged(setting.copy(value = option))
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = option)
                    }
                }
            }
        }
        is SettingType.Dialog -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDialog = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = setting.textToDisplay)
                Spacer(modifier = Modifier.weight(1f))
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
    }
}

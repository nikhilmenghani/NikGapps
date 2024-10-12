package com.nikgapps.ui.preferences

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.nikgapps.ui.theme.ThemePreference
import com.nikgapps.ui.preferences.modal.prefMutableState

const val emptyString = ""

class PreferencesManager {

    object SingleChoiceDialog {
        var show by mutableStateOf(false)

        var title = emptyString
            private set
        var description = emptyString
            private set
        var choices = mutableListOf<String>()
            private set
        var onSelect: (choice: Int) -> Unit = {}
            private set
        var selectedChoice = -1

        fun dismiss() {
            show = false
            title = emptyString
            description = emptyString
            choices.clear()
            selectedChoice = -1
            onSelect = {}
        }

        fun show(
            title: String,
            description: String,
            choices: List<String>,
            selectedChoice: Int,
            onSelect: (choice: Int) -> Unit
        ) {
            SingleChoiceDialog.title = title
            SingleChoiceDialog.description = description
            SingleChoiceDialog.choices.clear()
            SingleChoiceDialog.choices.addAll(choices)
            SingleChoiceDialog.onSelect = onSelect
            SingleChoiceDialog.selectedChoice = selectedChoice
            show = true
        }
    }

    object DisplayPrefs {
        var theme by prefMutableState(
            keyName = "theme",
            defaultValue = ThemePreference.SYSTEM.ordinal,
            getPreferencesKey = { intPreferencesKey(it) }
        )

        var useDynamicColor by prefMutableState(
            keyName = "useDynamicColor",
            defaultValue = true,
            getPreferencesKey = { booleanPreferencesKey(it) }
        )

        var fileListColumnCount by prefMutableState(
            keyName = "fileListColumnCount",
            defaultValue = 1,
            getPreferencesKey = { intPreferencesKey(it) }
        )
    }


    val displayPrefs = DisplayPrefs
    val singleChoiceDialog = SingleChoiceDialog

}
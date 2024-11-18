package com.nikgapps.app.data.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.nikgapps.app.utils.managers.prefMutableState

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

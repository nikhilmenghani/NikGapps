package com.nikgapps.app.data

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

    var developerOptionsEnabled by prefMutableState(
        keyName = "developerOptionsEnabled",
        defaultValue = false,
        getPreferencesKey = { booleanPreferencesKey(it) }
    )

    var allowUnsupportedAndroidVersions by prefMutableState(
        keyName = "allowUnsupportedAndroidVersions",
        defaultValue = false,
        getPreferencesKey = { booleanPreferencesKey(it) }
    )

    var biometricLockEnabled by prefMutableState(
        keyName = "biometricLockEnabled",
        defaultValue = false,
        getPreferencesKey = { booleanPreferencesKey(it) }
    )
}

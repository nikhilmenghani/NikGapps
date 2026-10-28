package com.nikgapps.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.nikgapps.app.utils.managers.prefMutableState

object DisplayPrefs {
    var useRadialProjectMenu by prefMutableState(
        keyName = "useRadialProjectMenu",
        defaultValue = true,
        getPreferencesKey = { booleanPreferencesKey(it) }
    )

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

    var allowGuestAccess by prefMutableState(
        keyName = "allowGuestAccess",
        defaultValue = false,
        getPreferencesKey = { booleanPreferencesKey(it) }
    )

    val guestAccessEnabled: Boolean get() = developerOptionsEnabled && allowGuestAccess

    var previewNonElite by prefMutableState(
        keyName = "previewNonElite",
        defaultValue = false,
        getPreferencesKey = { booleanPreferencesKey(it) }
    )

    val nonElitePreviewEnabled: Boolean
        get() = nonElitePreviewEnabled(developerOptionsEnabled, previewNonElite)

    private var storedAllowUnsupportedAndroidVersions by prefMutableState(
        keyName = "allowUnsupportedAndroidVersions",
        defaultValue = false,
        getPreferencesKey = { booleanPreferencesKey(it) }
    )

    private var storedBiometricLockEnabled by prefMutableState(
        keyName = "biometricLockEnabled",
        defaultValue = false,
        getPreferencesKey = { booleanPreferencesKey(it) }
    )

    private var storedRequireInternetAccess by prefMutableState(
        keyName = "requireInternetAccess",
        defaultValue = true,
        getPreferencesKey = { booleanPreferencesKey(it) }
    )

    private var storedEnforceAppUpdates by prefMutableState(
        keyName = "enforceAppUpdates",
        defaultValue = true,
        getPreferencesKey = { booleanPreferencesKey(it) }
    )

    var allowUnsupportedAndroidVersions: Boolean
        get() = developerOptionValue(developerOptionsEnabled, storedAllowUnsupportedAndroidVersions, false)
        set(value) { storedAllowUnsupportedAndroidVersions = value }

    var biometricLockEnabled: Boolean
        get() = developerOptionValue(developerOptionsEnabled, storedBiometricLockEnabled, false)
        set(value) { storedBiometricLockEnabled = value }

    var requireInternetAccess: Boolean
        get() = developerOptionValue(developerOptionsEnabled, storedRequireInternetAccess, true)
        set(value) { storedRequireInternetAccess = value }

    var enforceAppUpdates: Boolean
        get() = developerOptionValue(developerOptionsEnabled, storedEnforceAppUpdates, true)
        set(value) { storedEnforceAppUpdates = value }

    fun hideDeveloperOptions() {
        // Disable first so no hidden override remains effective while preferences persist.
        developerOptionsEnabled = false
        previewNonElite = false
        allowGuestAccess = false
        GithubPrefs.guestMode = false
        allowUnsupportedAndroidVersions = false
        biometricLockEnabled = false
        requireInternetAccess = true
        enforceAppUpdates = true
    }
}

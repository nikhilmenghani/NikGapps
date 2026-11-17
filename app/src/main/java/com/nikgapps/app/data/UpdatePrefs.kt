package com.nikgapps.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nikgapps.app.utils.managers.prefMutableState

object UpdatePrefs {
    internal var lastSuccessfulDevelopmentRelease by prefMutableState(
        keyName = "nikgapps_releases_last_development_version",
        defaultValue = "",
        getPreferencesKey = { stringPreferencesKey(it) }
    )

    internal var lastSuccessfulStableRelease by prefMutableState(
        keyName = "nikgapps_releases_last_stable_version",
        defaultValue = "",
        getPreferencesKey = { stringPreferencesKey(it) }
    )

    var intervalHours by prefMutableState(
        keyName = "updateCheckIntervalHours",
        defaultValue = 24,
        getPreferencesKey = { intPreferencesKey(it) }
    )
}

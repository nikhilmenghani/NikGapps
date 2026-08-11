package com.nikgapps.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.intPreferencesKey
import com.nikgapps.app.utils.managers.prefMutableState

object UpdatePrefs {
    var intervalHours by prefMutableState(
        keyName = "updateCheckIntervalHours",
        defaultValue = 24,
        getPreferencesKey = { intPreferencesKey(it) }
    )
}

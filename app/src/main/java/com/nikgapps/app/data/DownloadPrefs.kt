package com.nikgapps.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.intPreferencesKey
import com.nikgapps.app.utils.managers.prefMutableState

object DownloadPrefs {
    var gappsVariant by prefMutableState(
        keyName = "gappsVariant",
        defaultValue = GappsVariantPreference.CORE.ordinal,
        getPreferencesKey = { intPreferencesKey(it) }
    )
}
package com.nikgapps.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nikgapps.app.utils.managers.emptyString
import com.nikgapps.app.utils.managers.prefMutableState

object GithubPrefs {
    var token by prefMutableState(
        keyName = "token",
        defaultValue = emptyString,
        getPreferencesKey = { stringPreferencesKey(it) }
    )
}
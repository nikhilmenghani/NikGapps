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
    var username by prefMutableState(
        keyName = "github_username",
        defaultValue = emptyString,
        getPreferencesKey = { stringPreferencesKey(it) }
    )
    var lastUsername by prefMutableState(
        keyName = "github_last_username",
        defaultValue = emptyString,
        getPreferencesKey = { stringPreferencesKey(it) }
    )
    var avatarUrl by prefMutableState(
        keyName = "github_avatar_url",
        defaultValue = emptyString,
        getPreferencesKey = { stringPreferencesKey(it) }
    )
}

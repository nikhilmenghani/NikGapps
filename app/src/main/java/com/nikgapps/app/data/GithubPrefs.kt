package com.nikgapps.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.nikgapps.app.utils.managers.emptyString
import com.nikgapps.app.utils.managers.prefMutableState

object GithubPrefs {
    var guestMode by prefMutableState(
        keyName = "github_guest_mode",
        defaultValue = false,
        getPreferencesKey = { booleanPreferencesKey(it) }
    )

    fun continueAsGuest() {
        check(DisplayPrefs.guestAccessEnabled) { "Guest access must be enabled in developer options" }
        if (username.isNotBlank()) lastUsername = username
        token = ""
        username = ""
        avatarUrl = ""
        guestMode = true
    }

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

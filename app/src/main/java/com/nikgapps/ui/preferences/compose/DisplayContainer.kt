package com.nikgapps.ui.preferences.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.R
import com.nikgapps.ui.preferences.constant.ThemePreference

@Composable
fun DisplayContainer() {
    val manager = globalClass.preferencesManager
    val preferences = manager.displayPrefs

    Container(title = stringResource(R.string.display)) {
        PreferenceItem(
            label = stringResource(R.string.theme),
            supportingText = when (preferences.theme) {
                ThemePreference.LIGHT.ordinal -> stringResource(R.string.light)
                ThemePreference.DARK.ordinal -> stringResource(R.string.dark)
                else -> stringResource(R.string.follow_system)
            },
            icon = Icons.Rounded.Nightlight,
            onClick = {
                manager.singleChoiceDialog.show(
                    title = globalClass.getString(R.string.theme),
                    description = globalClass.getString(R.string.select_theme_preference),
                    choices = listOf(
                        globalClass.getString(R.string.light),
                        globalClass.getString(R.string.dark),
                        globalClass.getString(R.string.follow_system)
                    ),
                    selectedChoice = preferences.theme,
                    onSelect = { preferences.theme = it }
                )
            }
        )

        val columnCount = arrayListOf(
            "1", "2", "3", "4", "Auto"
        )

    }
}
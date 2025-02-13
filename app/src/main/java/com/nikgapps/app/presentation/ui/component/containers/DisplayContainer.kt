package com.nikgapps.app.presentation.ui.component.containers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.R
import com.nikgapps.app.data.ThemePreference
import com.nikgapps.app.presentation.ui.component.items.PreferenceItem
import com.nikgapps.app.utils.managers.emptyString

@Composable
fun DisplayContainer() {
    val dialog = globalClass.singleChoiceDialog
    val textDialog = globalClass.singleTextDialog
    val preferences = globalClass.preferencesManager.displayPrefs
    val githubPreference = globalClass.preferencesManager.githubPrefs

    Container(title = stringResource(R.string.display)) {
        PreferenceItem(
            label = stringResource(R.string.use_dynamic_color),
            supportingText = emptyString,
            icon = Icons.AutoMirrored.Rounded.Label,
            switchState = preferences.useDynamicColor,
            onSwitchChange = { preferences.useDynamicColor = it }
        )

        if (!preferences.useDynamicColor) {
            PreferenceItem(
                label = stringResource(R.string.theme),
                supportingText = when (preferences.theme) {
                    ThemePreference.LIGHT.ordinal -> stringResource(R.string.light)
                    ThemePreference.DARK.ordinal -> stringResource(R.string.dark)
                    else -> stringResource(R.string.follow_system)
                },
                icon = Icons.Rounded.Nightlight,
                onClick = {
                    dialog.show(
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
        }
    }

    Container(title = "Authentication") {
        PreferenceItem(
            label = "Github Token",
            supportingText = githubPreference.token,
            icon = Icons.AutoMirrored.Rounded.Label,
            onClick = {
                textDialog.show(
                    title = "Github Token",
                    description = "Enter your Github token",
                    text = githubPreference.token,
                    onConfirm = { githubPreference.token = it }
                )
            }
        )
    }
}
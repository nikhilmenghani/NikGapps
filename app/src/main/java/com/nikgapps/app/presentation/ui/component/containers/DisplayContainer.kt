package com.nikgapps.app.presentation.ui.component.containers

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AppSettingsAlt
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Science
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.R
import com.nikgapps.app.data.DownloadPrefs
import com.nikgapps.app.data.GappsVariantPreference
import com.nikgapps.app.data.ThemePreference
import com.nikgapps.app.data.toVariantString
import com.nikgapps.app.presentation.ui.component.items.PreferenceItem
import com.nikgapps.app.presentation.ui.component.items.PreferenceSubtitle
import com.nikgapps.app.utils.managers.emptyString
import com.nikgapps.app.update.AppUpdateManager

@Composable
fun AppearancePreferences() {
    val dialog = globalClass.singleChoiceDialog
    val preferences = globalClass.preferencesManager.displayPrefs

    SettingsPage {
        Container(
            title = stringResource(R.string.settings_appearance),
            initiallyExpanded = true
        ) {
            PreferenceSubtitle(text = stringResource(R.string.settings_colors_and_theme))
            PreferenceItem(
                label = stringResource(R.string.use_dynamic_color),
                supportingText = emptyString,
                icon = Icons.Outlined.ColorLens,
                switchState = preferences.useDynamicColor,
                onSwitchChange = { preferences.useDynamicColor = it }
            )

            PreferenceItem(
                    label = stringResource(R.string.theme),
                    supportingText = when (preferences.theme) {
                        ThemePreference.LIGHT.ordinal -> stringResource(R.string.light)
                        ThemePreference.DARK.ordinal -> stringResource(R.string.dark)
                        else -> stringResource(R.string.follow_system)
                    },
                    icon = Icons.Outlined.DarkMode,
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
}

@Composable
fun AdvancedPreferences() {
    val dialog = globalClass.singleChoiceDialog
    val textDialog = globalClass.singleTextDialog
    val githubPreference = globalClass.preferencesManager.githubPrefs
    val developerPreference = globalClass.preferencesManager.displayPrefs

    SettingsPage {
        Container(
            title = stringResource(R.string.settings_advanced),
            initiallyExpanded = true
        ) {
            PreferenceSubtitle(text = stringResource(R.string.settings_download_defaults))
            PreferenceItem(
                label = stringResource(R.string.gapps_variant),
                supportingText = GappsVariantPreference.entries[
                    DownloadPrefs.gappsVariant.coerceIn(GappsVariantPreference.entries.indices)
                ].toVariantString(),
                icon = Icons.Outlined.Cloud,
                onClick = {
                    dialog.show(
                        title = globalClass.getString(R.string.gapps_variant),
                        description = globalClass.getString(R.string.select_variant_preference),
                        choices = GappsVariantPreference.entries.map { it.toVariantString() },
                        selectedChoice = DownloadPrefs.gappsVariant,
                        onSelect = { DownloadPrefs.gappsVariant = it }
                    )
                }
            )

            if (developerPreference.developerOptionsEnabled) {
                PreferenceSubtitle(text = "Developer options")
                PreferenceItem(
                    label = "Allow unsupported Android versions",
                    supportingText = "Show Android versions without published package metadata",
                    icon = Icons.Outlined.Science,
                    switchState = developerPreference.allowUnsupportedAndroidVersions,
                    onSwitchChange = { developerPreference.allowUnsupportedAndroidVersions = it }
                )
                PreferenceItem(
                    label = "Hide developer options",
                    supportingText = "Tap the app version seven times to enable them again",
                    icon = Icons.Outlined.Android,
                    onClick = {
                        developerPreference.allowUnsupportedAndroidVersions = false
                        developerPreference.developerOptionsEnabled = false
                    }
                )
            }

            PreferenceSubtitle(text = stringResource(R.string.settings_authentication))
            PreferenceItem(
                label = stringResource(R.string.settings_github_token),
                supportingText = if (githubPreference.token.isBlank()) {
                    stringResource(R.string.settings_not_configured)
                } else {
                    stringResource(R.string.settings_configured)
                },
                icon = Icons.Outlined.Key,
                onClick = {
                    textDialog.show(
                        title = globalClass.getString(R.string.settings_github_token),
                        description = globalClass.getString(R.string.settings_github_token_description),
                        text = githubPreference.token,
                        onConfirm = { githubPreference.token = it.trim() }
                    )
                }
            )
        }
    }
}

@SuppressLint("BatteryLife")
@Composable
fun SystemPreferences(
    versionName: String,
    onPermissionsClick: () -> Unit,
    onAppSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val dialog = globalClass.singleChoiceDialog
    val updatePrefs = globalClass.preferencesManager.updatePrefs
    val developerPreference = globalClass.preferencesManager.displayPrefs
    var developerTapCount by remember { mutableStateOf(0) }
    val powerManager = remember { context.getSystemService(PowerManager::class.java) }
    var ignoresBatteryOptimizations by remember {
        mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, powerManager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                ignoresBatteryOptimizations =
                    powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SettingsPage {
        Container(
            title = stringResource(R.string.settings_system),
            initiallyExpanded = true
        ) {
            PreferenceSubtitle(text = stringResource(R.string.settings_device_access))
            PreferenceItem(
                label = stringResource(R.string.settings_permissions),
                supportingText = stringResource(R.string.settings_permissions_description),
                icon = Icons.Outlined.Security,
                onClick = onPermissionsClick
            )
            PreferenceItem(
                label = stringResource(R.string.settings_android_app_settings),
                supportingText = stringResource(R.string.settings_android_app_settings_description),
                icon = Icons.Outlined.AppSettingsAlt,
                onClick = onAppSettingsClick
            )

            PreferenceSubtitle(text = stringResource(R.string.settings_background))
            PreferenceItem(
                label = "Automatic update checks",
                supportingText = updateIntervalLabel(updatePrefs.intervalHours),
                icon = Icons.Outlined.SystemUpdate,
                onClick = {
                    val intervals = listOf(0, 12, 24, 168)
                    dialog.show(
                        title = "Automatic update checks",
                        description = "Choose how often NikGapps checks for a new app release",
                        choices = intervals.map(::updateIntervalLabel),
                        selectedChoice = intervals.indexOf(updatePrefs.intervalHours).coerceAtLeast(0),
                        onSelect = { index ->
                            updatePrefs.intervalHours = intervals[index]
                            AppUpdateManager.scheduleChecks(context, intervals[index])
                            if (intervals[index] > 0) AppUpdateManager.checkOnAppStart(context)
                        }
                    )
                }
            )
            PreferenceItem(
                label = stringResource(R.string.settings_battery_optimization),
                supportingText = stringResource(
                    if (ignoresBatteryOptimizations) R.string.settings_battery_optimization_allowed
                    else R.string.settings_battery_optimization_restricted
                ),
                icon = Icons.Outlined.BatterySaver,
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            )

            PreferenceSubtitle(text = stringResource(R.string.settings_about))
            PreferenceItem(
                label = stringResource(R.string.app_name),
                supportingText = stringResource(R.string.settings_version, versionName),
                icon = Icons.Outlined.Android,
                onClick = {
                    if (developerPreference.developerOptionsEnabled) {
                        Toast.makeText(context, "Developer options are already enabled", Toast.LENGTH_SHORT).show()
                    } else {
                        developerTapCount++
                        if (developerTapCount >= 7) {
                            developerPreference.developerOptionsEnabled = true
                            developerTapCount = 0
                            Toast.makeText(context, "Developer options enabled", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
            PreferenceItem(
                label = stringResource(R.string.settings_application_id),
                supportingText = "com.nikgapps",
                icon = Icons.Outlined.Badge
            )
        }
    }
}

private fun updateIntervalLabel(hours: Int) = when (hours) {
    0 -> "Off"
    12 -> "Every 12 hours"
    24 -> "Daily"
    168 -> "Weekly"
    else -> "Every $hours hours"
}

@Composable
private fun SettingsPage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Deprecated("Use the category-specific preference pages")
@Composable
fun DisplayContainer() {
    AppearancePreferences()
}

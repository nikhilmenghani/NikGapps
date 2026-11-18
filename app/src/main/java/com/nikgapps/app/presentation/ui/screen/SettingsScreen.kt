package com.nikgapps.app.presentation.ui.screen

import com.nikgapps.app.presentation.ui.component.glassTopAppBarColors
import com.nikgapps.app.presentation.ui.component.GlassScaffold
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.nikgapps.PermissionsActivity
import com.nikgapps.R
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.app.presentation.ui.component.containers.*
import com.nikgapps.app.presentation.ui.component.dialogs.SingleChoiceDialog
import com.nikgapps.app.presentation.ui.component.dialogs.SingleTextDialog
import com.nikgapps.app.update.ChangelogDialog
import com.nikgapps.app.update.ChangelogEntry
import com.nikgapps.app.update.ChangelogRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, showBackButton: Boolean = true) {
    val context = LocalContext.current
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    val developerOptionsEnabled = globalClass.preferencesManager.displayPrefs.developerOptionsEnabled
    var showChangelog by remember { mutableStateOf(false) }
    var changelog by remember { mutableStateOf<List<ChangelogEntry>>(emptyList()) }
    LaunchedEffect(Unit) { changelog = ChangelogRepository.fetch(context) }

    GlassScaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {
        TopAppBar(colors = glassTopAppBarColors(), title = { Text(stringResource(R.string.settings)) }, navigationIcon = {
            if (showBackButton) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            }
        })
    }) { padding ->
        SingleChoiceDialog()
        SingleTextDialog()
        if (showChangelog) ChangelogDialog(title = "NikGapps changelog", entries = changelog,
            loading = false, onDismiss = { showChangelog = false })
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppearancePreferences()
            AccountPreferences()
            SystemPreferences(onPermissionsClick = {
                context.startActivity(Intent(context, PermissionsActivity::class.java)
                    .putExtra(PermissionsActivity.EXTRA_REVIEW_MODE, true))
            }, onAppSettingsClick = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")))
            })
            AboutPreferences(versionName = versionName, onChangelogClick = { showChangelog = true })
            if (developerOptionsEnabled) AdvancedPreferences()
        }
    }
}

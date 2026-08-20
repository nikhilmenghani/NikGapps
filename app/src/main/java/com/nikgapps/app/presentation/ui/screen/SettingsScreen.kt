package com.nikgapps.app.presentation.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nikgapps.PermissionsActivity
import com.nikgapps.R
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.app.presentation.ui.component.containers.AdvancedPreferences
import com.nikgapps.app.presentation.ui.component.containers.AppearancePreferences
import com.nikgapps.app.presentation.ui.component.containers.SystemPreferences
import com.nikgapps.app.presentation.ui.component.dialogs.SingleChoiceDialog
import com.nikgapps.app.presentation.ui.component.dialogs.SingleTextDialog
import com.nikgapps.app.update.ChangelogDialog
import com.nikgapps.app.update.ChangelogEntry
import com.nikgapps.app.update.ChangelogRepository
import kotlinx.coroutines.launch

private enum class SettingsCategory(
    val titleRes: Int,
    val icon: ImageVector
) {
    APPEARANCE(R.string.settings_appearance, Icons.Outlined.Palette),
    SYSTEM(R.string.settings_system, Icons.Outlined.PhoneAndroid),
    ADVANCED(R.string.settings_advanced, Icons.Outlined.Tune);

    fun isApplicable(developerOptionsEnabled: Boolean): Boolean =
        this != ADVANCED || developerOptionsEnabled
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    val developerOptionsEnabled = globalClass.preferencesManager.displayPrefs.developerOptionsEnabled
    val visibleCategories = SettingsCategory.entries.filter {
        it.isApplicable(developerOptionsEnabled)
    }
    val pagerState = rememberPagerState(pageCount = { visibleCategories.size })
    var selectedPage by rememberSaveable { mutableIntStateOf(0) }
    var showChangelog by remember { mutableStateOf(false) }
    var changelogLoading by remember { mutableStateOf(false) }
    var changelog by remember { mutableStateOf<List<ChangelogEntry>>(emptyList()) }
    val useSideNavigation = LocalConfiguration.current.screenWidthDp >= 600

    LaunchedEffect(pagerState.currentPage) {
        selectedPage = pagerState.currentPage
    }

    LaunchedEffect(visibleCategories.size) {
        if (pagerState.currentPage !in visibleCategories.indices) {
            val fallbackPage = visibleCategories.lastIndex
            pagerState.scrollToPage(fallbackPage)
            selectedPage = fallbackPage
        }
    }

    fun selectCategory(category: SettingsCategory) {
        val page = visibleCategories.indexOf(category)
        if (page < 0) return
        selectedPage = page
        scope.launch { pagerState.animateScrollToPage(page) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (!useSideNavigation) {
                SettingsBottomNavigation(
                    categories = visibleCategories,
                    currentCategory = visibleCategories[selectedPage.coerceIn(visibleCategories.indices)],
                    onCategorySelected = ::selectCategory
                )
            }
        }
    ) { paddingValues ->
        SingleChoiceDialog()
        SingleTextDialog()
        if (showChangelog) {
            ChangelogDialog(
                title = "NikGapps changelog",
                entries = changelog,
                loading = changelogLoading,
                onDismiss = { showChangelog = false }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (useSideNavigation) {
                SettingsSideNavigation(
                    categories = visibleCategories,
                    currentCategory = visibleCategories[selectedPage.coerceIn(visibleCategories.indices)],
                    onCategorySelected = ::selectCategory
                )
                VerticalDivider(modifier = Modifier.fillMaxHeight())
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) { page ->
                when (visibleCategories[page]) {
                    SettingsCategory.APPEARANCE -> AppearancePreferences()
                    SettingsCategory.ADVANCED -> AdvancedPreferences()
                    SettingsCategory.SYSTEM -> SystemPreferences(
                        versionName = versionName,
                        onPermissionsClick = {
                            context.startActivity(
                                Intent(context, PermissionsActivity::class.java)
                                    .putExtra(PermissionsActivity.EXTRA_REVIEW_MODE, true)
                            )
                        },
                        onAppSettingsClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        },
                        onChangelogClick = {
                            showChangelog = true
                            changelogLoading = true
                            scope.launch {
                                changelog = ChangelogRepository.fetch()
                                changelogLoading = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsBottomNavigation(
    categories: List<SettingsCategory>,
    currentCategory: SettingsCategory,
    onCategorySelected: (SettingsCategory) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 448.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            categories.forEach { category ->
                SettingsNavigationItem(
                    category = category,
                    selected = currentCategory == category,
                    onClick = { onCategorySelected(category) },
                    modifier = if (currentCategory == category) Modifier.weight(1f)
                    else Modifier.width(64.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSideNavigation(
    categories: List<SettingsCategory>,
    currentCategory: SettingsCategory,
    onCategorySelected: (SettingsCategory) -> Unit
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        categories.forEach { category ->
            SettingsNavigationItem(
                category = category,
                selected = currentCategory == category,
                onClick = { onCategorySelected(category) },
                modifier = Modifier.fillMaxWidth(),
                alwaysShowLabel = true
            )
        }
    }
}

@Composable
private fun SettingsNavigationItem(
    category: SettingsCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alwaysShowLabel: Boolean = false
) {
    val containerColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "settingsNavigationContainer"
    )
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "settingsNavigationContent"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = if (selected || alwaysShowLabel) null
                else stringResource(category.titleRes),
                modifier = Modifier.size(24.dp)
            )
            AnimatedVisibility(
                visible = selected || alwaysShowLabel,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Text(
                    text = stringResource(category.titleRes),
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

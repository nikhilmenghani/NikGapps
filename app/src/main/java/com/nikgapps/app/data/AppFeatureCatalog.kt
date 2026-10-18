package com.nikgapps.app.data

data class AppFeature(val title: String, val summary: String, val details: String, val elite: Boolean = false)
data class AppFeaturePage(val title: String, val description: String, val features: List<AppFeature>)

/** Add features and pages here; the guide renders them without UI changes. */
object AppFeatureCatalog {
    val eliteFeatures = listOf(
        AppFeature("Elite badge", "Recognition on your dashboard", "Sign in with an eligible GitHub account to see your Elite badge.", true),
        AppFeature("Extra builds", "Add six builds to your remaining allowance", "Use Reset build window on the project screen. Your remaining allowance plus six starts a new six-hour window. Available once per six-hour window.", true),
        AppFeature("Compression control", "Choose ZIP compression levels 1–9", "Enable compressed builds in Settings → System, then choose a compression level. Higher levels prioritize smaller files over build speed.", true),
        AppFeature("ZIP filename date", "Release date or current date", "Settings → System lets you choose the release date or today's device-local date for the filename. Package versions and release metadata stay unchanged.", true),
        AppFeature("Longer project names", "25 characters instead of 20", "Elite accounts can create and rename projects with up to 25 characters. Regular accounts have a 20-character limit.", true)
    )
    val pages = listOf(
        AppFeaturePage("Projects", "Organize your own GApps selections", listOf(
            AppFeature("Create a project", "Name it and choose Android", "Tap + on Home, enter a project name and select a supported Android version. ARM64 is the supported architecture. Optionally load a config to preselect packages."),
            AppFeature("Manage projects", "Edit, copy, delete and reopen ZIPs", "Long-press a project card for its menu. Rename or copy a project, delete one you no longer need, or reopen its previously built ZIP using Open ZIP."),
            AppFeature("Outdated projects", "Keep old selections separate", "Outdated projects appear below active projects with a tinted card and badge. Tap to reveal actions, then Open to review the project and available recovery options. Outdated projects cannot be built until recreated with current definitions.")
        )),
        AppFeaturePage("Packages", "Select exactly what you need", listOf(
            AppFeature("AppSets or Packages", "Choose your preferred view", "Switch between grouped AppSets and individual Packages on the apps screen. Expand an AppSet to see its packages, or select the whole group."),
            AppFeature("Selection shortcuts", "Select, clear or invert", "Expand the selected-apps summary to use bulk selection actions. Core and CoreGo are mutually exclusive, as are SetupWizard and PixelSetupWizard. Select all prefers Core and SetupWizard."),
            AppFeature("Package info", "Press and hold for more options", "Tap a package tile to toggle selection. Press and hold it and choose Show info to inspect package details. Search, sort and filters help locate packages; Installed badges identify apps on your device."),
            AppFeature("Keep Stock app", "Keep the original system app", "Long-press a selected package and choose Keep Stock app to preserve its stock counterpart during installation. A red edge marks the preference. Choose Delete Stock app to restore normal removal; the generated config uses value 2 when keeping it.")
        )),
        AppFeaturePage("Configs", "Share and reuse your selection", listOf(
            AppFeature("Project debloater", "Bundle optional app removals", "Open Debloater on Choose your apps. Add each file or folder with +, review the list, acknowledge the warning and Apply. The project config preserves your targets. Removals happen when flashing the ZIP, not in the app. Known critical components and conflicting selections are blocked; other removals can still break Android."),
            AppFeature("Load a config", "Optional setup with immediate validation", "On Create project, choose Android first, then Load config. Only .config files matching the selected Android version and latest supported definitions are accepted. Invalid configs block creation until removed or replaced. Remove the config to unlock Android selection."),
            AppFeature("Export a config", "Save your selection as a .config file", "Choose Export config when packages are selected. The filename defaults to the project name and can be renamed. Exports use LF line endings, the Android-specific published template, and UseZipConfig enabled."),
            AppFeature("Stock-app preferences", "Your choices travel with the config", "Value 0 disables a package, 1 selects it, and 2 selects it while keeping its stock app. Import and export preserve these preferences. Required dependencies are resolved automatically.")
        )),
        AppFeaturePage("ZIP builds", "Create a flashable GApps package", listOf(
            AppFeature("Build your ZIP", "Use your project's selected packages", "Choose Build ZIP or Create ZIP to assemble a package from the published registry. A connection is required to verify metadata and fetch package files. The normal build quota applies."),
            AppFeature("Build progress", "Track the full operation", "The build screen shows overall progress alongside package download progress and logs. Completed ZIPs are saved to Downloads/NikGapps and can be reopened from Home."),
            AppFeature("Standard or compressed", "Build speed or smaller files", "Settings → System lets you choose standard builds or compressed builds. Compression usually creates smaller ZIPs but takes longer. The optional build prompt can remember your choice.")
        )),
        AppFeaturePage("Settings", "Make NikGapps work your way", listOf(
            AppFeature("Appearance", "Customize the app's look", "Open Settings → Appearance to adjust the available theme and display preferences."),
            AppFeature("Updates and changelog", "Stay informed about new releases", "Settings → System offers automatic update-check intervals. Settings → About has the cached changelog. Expand release rows, use Show all for longer lists, or Refresh to download the latest changelog."),
            AppFeature("Background operation", "Review permissions and battery access", "Use Settings → System to review device access and battery optimization settings so background operations can continue reliably.")
        )),
        AppFeaturePage("Elite features", "Extra options for eligible GitHub accounts", eliteFeatures)
    )
}

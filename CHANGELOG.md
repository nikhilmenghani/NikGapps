# NikGapps changelog

## 0.80.21
- Bundle project-specific debloating with standard commented configs in build ZIPs.
- Choose compact radial or classic list project menus in Settings.
- Use standalone build, export and debloater buttons with a configured badge.
- Place Load config and Create project side by side.
- Fix Back navigation so returning Home clears screen history.
- Keep project ordering and welcome-card layout stable while loading.
- Show project status through icon colors and improve loaded-config feedback.
- Move Settings and Logs into the menu, with a separate create-project button.
- Fix clipped Settings tabs and align the welcome icon for regular accounts.
- Keep Installed badges on one line across display and text sizes.
- Fix storage permission requests on older Android versions.
- Show release announcements only for compatible projects with available updates.

## 0.80.20
- Discover app features in a paged guide; About now has its own Settings tab.
- Group Battery Optimization under Device Access.
- Expand only the newest changelog release by default.

## 0.80.19
- Fix the Pixel Setup Wizard crashes on a17 - marking a17 Gapps stable
- Get dashboard and background notifications when a new NikGapps package release is available.
- See update badges for projects matching the release's Android version, channel, and architecture.
- Build compatible projects with the latest packages; clear their update badge after the ZIP is successfully saved.
- Export incompatible project configurations, edit affected entries, and import them when creating a new project.
- Dismiss release announcements separately from individual project updates; newer releases appear again.
- Automatically hide release announcements and notifications when a project already uses that release.
- Dismiss updates for all compatible projects awaiting the same release or an older one.
- Use matching rounded Keep and Dismiss buttons in release and project confirmation dialogs.
- Show a compact, expandable release card with a consistent UTC release date and device theme colors.
- Move project actions into a rounded press-and-hold menu that opens near the touch position, including ZIP sharing.
- Load cached release and project status first, then refresh in the background for a faster dashboard.

## 0.80.18
- Add Elite ZIP date options, 25-character project names, and a feature overview; starred settings stay visible when locked.
- Fixed Bootloop caused by Google Chrome and Google Clock on A17
- Import and export package selections using the latest Android-specific config template.
- Tap to select packages; long-press for info and Keep Stock app options.
- Switch between AppSets and Packages, with exclusive Core and SetupWizard choices.
- Compact selection cards with Installed badges and Keep Stock app indicators.
- Show active projects first; tint outdated cards and tap to reveal their actions.
- Keep dashboard headers fixed and restore scroll position after viewing package info.
- Smooth navigation and build-button transitions; improve Elite badge placement.
- Browse a changelog timeline with five-item previews and manual refresh.

## 0.80.17
- Choose compressed builds for smaller ZIPs or standard builds for faster creation, in Settings or before starting a build.
- Elite users can choose compression levels 1–9, balancing ZIP size against build time.
- Elite users can reset their build window with remaining builds plus six.
- Fixed renaming a project clearing its selected apps and source settings.
- Increased the project-name limit from 20 to 25 characters.

## 0.80.16
- Update the a17 apps from Sept release
- Mark outdated projects as read-only and let users duplicate them without unavailable packages.
- Show overall build progress alongside the current package download progress.
- Show an Elite badge for eligible GitHub users.

## 0.80.15
- Fixed the search UI breaking on some devices

## 0.80.14
- Performance Improvements

## 0.80.13
- Fix the update icon showing weird badge with new update

## 0.80.12
- Implement showing changelog with new release

## 0.80.11
- Fixed the search button overlay making it difficult to select the app behind it.

## 0.80.10
- Minor Improvements

## 0.80.9
- Initial public release

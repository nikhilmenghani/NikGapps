# NikGapps UI conventions

- Follow the active app/device theme for every screen, dialog, bottom sheet and popup. Use `MaterialTheme.colorScheme` and `MaterialTheme.typography`; respect light/dark, dynamic-color and user-selected theme settings. Avoid hard-coded colors or independent popup palettes.
- Match related flows to existing project UI: compact spacing, rounded surfaces, and a horizontal divider between message/content and action buttons. Use 16dp button corners and a minimum 48dp touch height. For two actions, use equal widths with a tonal secondary and primary confirm button; for one action, use a full-width primary button.
- Reuse shared components so Choose your apps and Review Build stay visually consistent. Keep review screens read-only and avoid redundant selection highlights.
- Keep header action icons outlined with a consistent theme tint. Use the shared `AppIcons.Info` for information/help icons instead of filled information circles.
- Use vertical-only accordion transitions for expandable content. Do not animate the width.
- Keep explanatory settings text behind an adjacent information icon when the section has a simple title.
- Keep changelog entries concise and group minor visual changes into broader UI improvements.
- Record parked features and follow-ups in `TODO.md`; update their status when work resumes or completes.

## Required UI checks

- Apply these conventions to every new or changed component, including authentication, token entry, browser links, dialogs and sheets. Existing one-off Material defaults are not a reason to repeat inconsistent UI.
- Before editing UI, inspect the closest matching project flow and reuse its components or the same theme, spacing, action hierarchy and shapes. Canonical examples: `AccountGate.kt` for sign-in actions and informational popups, `GitHubTokenPreference.kt` for compact entry sheets with paired actions, and `ProjectCreationGate.kt` for single-action dialogs.
- Keep explanatory/help text hidden by default behind an accessible information icon next to the relevant title. Show it in a themed popup; do not add permanent help cards or expand the underlying layout unless the user explicitly asks for that presentation.
- A control opening a browser is still an app action: use the same button design. Do not use a plain text button or link as a substitute for a normal action. Use equal-width tonal secondary and primary confirmation buttons for two actions, 16dp corners, minimum 48dp touch targets, and a divider above the action area.
- Before reporting a UI change complete, check it against these rules and inspect the rendered screen on the requested connected device when available. Verify information is hidden initially, buttons match the project, keyboard-visible actions are reachable, and popup dismissal preserves the underlying layout. Fix discrepancies before handing it back to the user.

## Installer removal metadata

- Use `nikgapps-release/nikgapps_release/engine/build/PackageConstants.py:get_package_deletes` and package pipeline overrides as the source of stock-app removal rules. The Android app must consume the resolved package version's `install.removeFiles` metadata rather than duplicate these mappings in Kotlin.
- Refer to `nikgapps-operations/MIGRATION.md` before changing hosted registry or release workflows; migrated source code and the deployed runtime can be at different stages of the migration.

# NikGapps UI conventions

- Follow the active app/device theme for every screen, dialog, bottom sheet and popup. Use `MaterialTheme.colorScheme` and `MaterialTheme.typography`; respect light/dark, dynamic-color and user-selected theme settings. Avoid hard-coded colors or independent popup palettes.
- Match related flows to existing project UI: compact spacing, rounded surfaces, and a horizontal divider between message/content and action buttons. Use 16dp button corners and a minimum 48dp touch height. For two actions, use equal widths with a tonal secondary and primary confirm button; for one action, use a full-width primary button.
- Reuse shared components so Choose your apps and Review Build stay visually consistent. Keep review screens read-only and avoid redundant selection highlights.
- Use vertical-only accordion transitions for expandable content. Do not animate the width.
- Keep explanatory settings text behind an adjacent information icon when the section has a simple title.
- Keep changelog entries concise and group minor visual changes into broader UI improvements.
- Record parked features and follow-ups in `TODO.md`; update their status when work resumes or completes.

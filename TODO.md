# Parked features

## Project backup and restore

- [x] **Elite project selection:** regular restores select all compatible projects read-only; Elite users can choose projects. Both modes replace the current project list.

- [ ] **Elite-only incremental import:** add a separate, explicit option to preserve existing projects while importing selected compatible projects. Verify Elite eligibility, define duplicate/conflict handling, preview the result and retain atomic saves. Standard restore remains a full replacement; outdated projects cannot be imported.
- [ ] **Exact file-extension filtering:** revisit an optional in-app folder picker showing only `.config` or `.nikgapps` files. Keep Android's native document picker for now; MIME filters cannot reliably distinguish custom extensions.

Keep this file updated when a feature is parked, resumed or completed. Deferred
work is not a shipped changelog feature.

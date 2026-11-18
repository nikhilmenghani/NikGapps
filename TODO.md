# Parked features

## Build allowances

- [ ] **Restore and enforce allowances after reinstall:** use a trusted remote build ledger keyed by the full, versioned Android-ID-derived `device_id`, with verified GitHub account association where available. The Virtual MAC is a short display value and must not be a unique key or credential. Current quotas remain installation-local; PostHog events alone are not an authoritative quota service. Handle retries, concurrent builds, offline failures, signing-key changes and factory resets explicitly. Do not ship privileged credentials in the APK.

## Package downloads

- [ ] **Guest package downloads:** populate the Download destination with existing NikGapps packages and allow guests to download them. The navigation entry and placeholder are in place; package browsing, downloading and public guest access remain to be implemented.

## Project backup and restore

- [x] **Elite project selection:** regular restores select all compatible projects read-only; Elite users can choose projects. Both modes replace the current project list.

- [ ] **Elite-only incremental import:** add a separate, explicit option to preserve existing projects while importing selected compatible projects. Verify Elite eligibility, define duplicate/conflict handling, preview the result and retain atomic saves. Standard restore remains a full replacement; outdated projects require the explicit Elite import option.
- [ ] **Exact file-extension filtering:** revisit an optional in-app folder picker showing only `.config` or `.nikgapps` files. Keep Android's native document picker for now; MIME filters cannot reliably distinguish custom extensions.

Keep this file updated when a feature is parked, resumed or completed. Deferred
work is not a shipped changelog feature.

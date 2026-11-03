# Project backup compatibility

Settings → Account → Project backup exports a UTF-8 JSON file with `.nikgapps`
extension. Import it through Restore projects in another NikGapps installation.
Debug and release package IDs do not affect import compatibility.

## Format and release association

`formatVersion` and `minimumReaderVersion` define the wire contract; both start
at 1. Each shipped release supports format 1 until a migration explicitly adds a
new supported format. A release rejects unsupported newer formats before touching
stored projects. When adding non-backward-compatible project fields, increment the
format version and add an explicit migration plus tests; retain readers for older
formats. Never infer compatibility from `versionCode`: debug and stable builds use
different numbering schemes.

`createdWith.appVersion`, `versionCode`, `buildType`, and `exportedAt` record the
creating release and are displayed before restore. They are provenance, not a
restriction: a release can restore a newer app's backup when its format is still
supported. `projectsSha256` detects accidental modifications of the project array;
it is not a signature or encryption. Backups are plaintext project definitions.

All project fields are preserved except installation-specific source paths.
No GitHub tokens, build quotas, device settings, APK bytes, ZIP bytes or local ZIP
links are backed up. Restored projects retain their package definition/release
fingerprints and go through normal registry compatibility checks when opened.

## Restore behavior

Import validates the whole file, format, checksum and every project, then fetches
fresh registry metadata for each Android/channel/architecture combination. Network
failure or cached-only metadata stops restore without changing existing projects.
Compatibility uses the same definition and package-availability checks as the
project list, plus dependency resolution for the target API and architecture.

Exports preserve all projects, including outdated ones. Restore previews all
records, marks outdated projects, and imports every compatible
project by default. The full-screen restore page uses Home-style project cards;
only the project icon highlights selection. Compatible projects are read-only and
selected for regular users. Elite users may tap cards to choose a subset; Elite
status is reverified on confirmation. Both modes replace the current list; there
is no merging. Information and warning icons reveal the source/version details,
Elite selection guidance and outdated-project notices. Pressing Restore opens a
themed overwrite confirmation if any existing projects will be replaced. There is
no overwrite checkbox. Elite users can enable Import outdated projects (off by
default), then select outdated projects individually. These retain their original
definitions and need recovery before building. Regular users skip outdated projects.
An empty selection cannot erase projects.

Regular accounts may restore at most 5 compatible projects; larger restores
require verified Elite membership. This limit counts the replacement, not the old
plus new lists. The repository checks that the current project list still matches
the reviewed snapshot and commits one complete replacement. Concurrent changes,
active ZIP builds, cancellation, damaged files and failed compatibility checks stop
restore. Saved local ZIP links are cleared after replacement; rebuild ZIPs.

Back up in the debug build before uninstalling it. Install a release build containing
this feature, sign in as needed, then restore the saved file.

## Parked improvements

See [TODO.md](../TODO.md) for Elite-only incremental import and exact file-extension
filtering. Android's native document picker remains in use for now.

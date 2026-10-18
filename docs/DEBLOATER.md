# Project debloating

The **Debloater** action in the floating bottom dock on **Choose your apps**
opens a single input and a **+** button. Each valid addition updates the removal
preview; entries can be removed individually. A pending,
unadded input blocks Apply. Apply stores the validated preview with the project
without an extra confirmation checkbox; Cancel discards it. Standalone Import config and
Export preview buttons are currently hidden. The project's main config import
and export still preserve the debloater section. Editing this list does not
delete apps from the connected device.

Only Cancel and Apply close the editor. Outside taps, Back and swipe dismissal
are disabled so the draft cannot be accidentally dismissed. The list has no
count heading or Clear all action.

The preview appears above the input/+ row. Top-right information and warning
icons reveal the guidance cards on demand. Entries are case-sensitive:
`youtube` and `YouTube` can coexist, but identical entries are rejected.
Critical-component and selected-package safety checks remain case-insensitive.

The sheet and preview wrap short lists, with extra padding above the title.
As the list grows the sheet expands upward; long lists scroll within a capped
height, leaving the input and action buttons below the list.

Exported `nikgapps.config` files also include these entries after the existing
Python-compatible `# NikGapps debloater starts here` marker, one target per line
(no `=1` or `=2` flags). Import restores that section; importing a config without
it clears the project's removal list. ZIP assembly writes the same validated
targets to `afzc/debloater.config` and records them in the build manifest.

The Worker and ZIP assembler revalidate before producing a ZIP. The ZIP contains
one `afzc/debloater.config`, a selected-package protection list, a guarded
installer and the targets in `nikgapps/build-manifest.json`. `UseZipConfig=1`
selects the bundled configuration. The guard is included in the app, so testing
does not require publishing newer registry packages first.

Only exact app directories under supported system/product/system_ext `app` or
`priv-app` roots, and files/subdirectories within those apps, are accepted.
Arbitrary system files, partition roots,
traversal, whitespace, shell syntax, wildcards, oversized/duplicate lists and
known critical component names are rejected. The installer resolves canonical
paths, rejects unsafe symlinks, plans all targets before deletion and rechecks
each path immediately before removing it. Missing app-directory roots are
skipped. Matched paths use the existing Debloater property/addon.d mechanism.

Selected package APK folders and known stock-app aliases marked to keep are
protected. This is not an exhaustive Android safety database: an otherwise
valid removal can still break boot, setup, input or ROM features. A bare folder
name may match multiple supported app locations; use a full supported path to
narrow it. Removing a target from a later build does not restore deleted files.
OTA persistence depends on the ROM's addon.d support and existing addon scripts.

The canonical shell source is
`project/NikGapps/helper/assets/debloater_guard.sh`; Android carries an identical
copy in `app/src/main/res/raw/debloater_guard.sh`. Keep them synchronized. Python
regression tests compare the copies when both checkouts are present. The Python
builder exports this helper too; its code changes must be deployed separately
before Python-generated ZIPs use it.

Verification uses unit tests, disposable archives and a fake shell removal
function. No automated check flashes a ZIP or deletes system files on a device.

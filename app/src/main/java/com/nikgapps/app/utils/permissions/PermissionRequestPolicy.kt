package com.nikgapps.app.utils.permissions

/** Track request history per Android permission, never per UI group. */
internal fun permanentlyDeniedPermissions(
    permissions: List<String>,
    wasRequested: (String) -> Boolean,
    isGranted: (String) -> Boolean,
    shouldShowRationale: (String) -> Boolean
): Boolean = permissions.any { wasRequested(it) && !isGranted(it) && !shouldShowRationale(it) }

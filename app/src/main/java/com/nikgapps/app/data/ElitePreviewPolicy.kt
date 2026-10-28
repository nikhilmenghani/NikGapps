package com.nikgapps.app.data

/** A local developer preview can only remove Elite access, never grant it. */
fun nonElitePreviewEnabled(developerOptionsEnabled: Boolean, previewRequested: Boolean): Boolean =
    developerOptionValue(developerOptionsEnabled, previewRequested, false)

/** Hidden developer preferences always resolve to the normal app default. */
fun developerOptionValue(enabled: Boolean, requested: Boolean, defaultValue: Boolean): Boolean =
    if (enabled) requested else defaultValue

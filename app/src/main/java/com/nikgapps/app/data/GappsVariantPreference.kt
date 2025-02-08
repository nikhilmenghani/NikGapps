package com.nikgapps.app.data

enum class GappsVariantPreference {
    CORE,
    BASIC,
    OMNI,
    STOCK,
    FULL
}

fun GappsVariantPreference.toVariantString(): String {
    return when (this) {
        GappsVariantPreference.CORE -> "Core"
        GappsVariantPreference.BASIC -> "Basic"
        GappsVariantPreference.OMNI -> "Omni"
        GappsVariantPreference.STOCK -> "Stock"
        GappsVariantPreference.FULL -> "Full"
    }
}
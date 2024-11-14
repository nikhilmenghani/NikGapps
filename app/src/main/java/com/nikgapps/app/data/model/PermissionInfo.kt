package com.nikgapps.app.data.model

import android.provider.Settings

data class PermissionInfo(
    val permission: Array<String>,
    val rationale: String,
    val action: String = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PermissionInfo) return false

        if (!permission.contentEquals(other.permission)) return false
        if (rationale != other.rationale) return false

        return true
    }

    override fun hashCode(): Int {
        var result = permission.contentHashCode()
        result = 31 * result + rationale.hashCode()
        return result
    }
}
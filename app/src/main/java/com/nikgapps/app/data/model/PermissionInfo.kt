package com.nikgapps.app.data.model

data class PermissionInfo(
    val permission: Array<String>,
    val rationale: String
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
package com.nikgapps.app.registry

import com.nikgapps.app.utils.permissions.permanentlyDeniedPermissions
import org.junit.Assert.*
import org.junit.Test

class PermissionRequestPolicyTest {
    private val storage = listOf("read", "write")

    @Test fun previouslyRequestedReadDoesNotPermanentlyDenyNeverRequestedWrite() {
        assertFalse(permanentlyDeniedPermissions(storage, { it == "read" }, { it == "read" }, { false }))
    }
    @Test fun missingWriteWithNoRationaleIsPermanentAfterItWasRequested() {
        assertTrue(permanentlyDeniedPermissions(storage, { true }, { it == "read" }, { false }))
    }
    @Test fun retryableDenialDoesNotSendUserToSettings() {
        assertFalse(permanentlyDeniedPermissions(storage, { true }, { it == "read" }, { it == "write" }))
    }
    @Test fun completeGrantOverridesOldDenialHistory() {
        assertFalse(permanentlyDeniedPermissions(storage, { true }, { true }, { false }))
    }
}

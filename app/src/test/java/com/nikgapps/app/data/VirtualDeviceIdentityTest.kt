package com.nikgapps.app.data

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class VirtualDeviceIdentityTest {
    @Test fun matchesCast4kMacAndVersionedFullIdentity() {
        val identity = requireNotNull(VirtualDeviceIdentity.fromAndroidId("0123456789abcdef"))
        assertEquals("00:1E:99:40:32:AF", identity.virtualMac)
        assertEquals("nikgapps-v1-d624c5af4971f8ad464e685759ba64d7428afc19df479c6c348393719302ccde", identity.deviceId)
        // Fresh computation, with no app preferences, reproduces the reinstall identity.
        assertEquals(identity, VirtualDeviceIdentity.fromAndroidId("0123456789abcdef"))
        assertFalse(identity.properties().values.contains("0123456789abcdef"))
    }

    @Test fun fullIdDistinguishesDevicesEvenWhenShortMacCollides() {
        val first = requireNotNull(VirtualDeviceIdentity.fromAndroidId("0000000000000168"))
        val second = requireNotNull(VirtualDeviceIdentity.fromAndroidId("0000000000000dcc"))
        assertEquals(first.virtualMac, second.virtualMac)
        assertNotEquals(first.deviceId, second.deviceId)
    }

    @Test fun unavailableIdsNeverBecomeSharedDeviceIdentity() {
        for (invalid in listOf(null, "", " ", "unknown", "0000000000000000", "0", "9774d56d682e549c", "0123456789abcdef0")) {
            assertNull(invalid, VirtualDeviceIdentity.fromAndroidId(invalid))
        }
    }

    @Test fun stableAcrossCaseWhitespaceAndLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals(VirtualDeviceIdentity.fromAndroidId("0123456789abcdef"),
                VirtualDeviceIdentity.fromAndroidId(" 0123456789ABCDEF "))
        } finally { Locale.setDefault(original) }
    }
}

package com.nikgapps.app.data

import android.content.Context
import android.annotation.SuppressLint
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale

/** App/signing-key and Android-user scoped identity. Changes after a factory reset.
 * The short Cast4K-style MAC is for display; use the full ID for correlation.
 * Neither value is a credential or proof of device ownership.
 */
data class VirtualDeviceIdentity(val deviceId: String, val virtualMac: String) {
    fun properties(): Map<String, Any> = mapOf(
        "device_id" to deviceId,
        "virtual_mac" to virtualMac,
        "device_identity_version" to 1
    )

    companion object {
        // Intentional app-scoped correlation across reinstall; never send the raw Android ID.
        @SuppressLint("HardwareIds")
        fun read(context: Context): VirtualDeviceIdentity? = fromAndroidId(
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        )

        internal fun fromAndroidId(value: String?): VirtualDeviceIdentity? {
            val androidId = value?.trim()?.lowercase(Locale.ROOT) ?: return null
            // Do not map unavailable/broken platform IDs to a shared device identity.
            if (!androidId.matches(Regex("[0-9a-f]{1,16}")) || androidId.all { it == '0' } ||
                androidId == "9774d56d682e549c") return null
            val legacyHash = digest("MD5", androidId)
            val mac = "00:1E:99:" + legacyHash.take(6).chunked(2).joinToString(":").uppercase(Locale.ROOT)
            val id = "nikgapps-v1-" + digest("SHA-256", "NikGapps:device:v1:$androidId")
            return VirtualDeviceIdentity(id, mac)
        }

        private fun digest(algorithm: String, value: String): String =
            MessageDigest.getInstance(algorithm).digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
    }
}

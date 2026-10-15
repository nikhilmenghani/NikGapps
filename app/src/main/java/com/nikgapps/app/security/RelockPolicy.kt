package com.nikgapps.app.security

/** Relock after 30s background or 5m since authentication; new processes always start locked. */
class RelockPolicy(private val backgroundTimeout: Long = 30_000, private val sessionTimeout: Long = 300_000) {
    private var authenticatedAt: Long? = null
    private var backgroundedAt: Long? = null
    fun authenticate(now: Long) { authenticatedAt = now; backgroundedAt = null }
    fun background(now: Long) { if (backgroundedAt == null) backgroundedAt = now }
    fun resume(now: Long): Boolean = unlocked(now).also { if (it) backgroundedAt = null }
    fun unlocked(now: Long): Boolean {
        val start = authenticatedAt ?: return false
        return now >= start && now - start < sessionTimeout &&
            (backgroundedAt?.let { now >= it && now - it < backgroundTimeout } ?: true)
    }
}

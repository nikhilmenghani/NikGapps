package com.nikgapps.app.analytics

import android.app.Application
import com.nikgapps.BuildConfig
import com.posthog.PersonProfiles
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

/** Privacy-minimal, anonymous product analytics. */
object AppAnalytics {
    private var initialized = false

    fun initialize(application: Application) {
        if (initialized || BuildConfig.POSTHOG_API_KEY.isBlank()) return
        val config = PostHogAndroidConfig(
            apiKey = BuildConfig.POSTHOG_API_KEY,
            host = BuildConfig.POSTHOG_HOST
        ).apply {
            debug = false
            captureApplicationLifecycleEvents = false
            captureScreenViews = false
            captureDeepLinks = false
            sessionReplay = false
            errorTrackingConfig.autoCapture = false
            preloadFeatureFlags = false
            sendFeatureFlagEvent = false
            personProfiles = PersonProfiles.NEVER
            optOut = false
        }
        PostHogAndroid.setup(application, config)
        initialized = true
    }

    fun zipCreationSucceeded(
        zipName: String,
        packageCount: Int,
        sizeBytes: Long,
        conflictResolution: String
    ) = capture(
        "zip_creation_succeeded",
        mapOf(
            "zip_name" to zipName,
            "package_count" to packageCount,
            "size_bytes" to sizeBytes,
            "size_mb" to sizeBytes.toDouble() / 1_048_576,
            "conflict_resolution" to conflictResolution
        )
    )

    fun zipCreationFailed(
        zipName: String,
        packageCount: Int,
        sizeBytes: Long,
        error: Throwable
    ) = capture(
        "zip_creation_failed",
        mapOf(
            "zip_name" to zipName,
            "package_count" to packageCount,
            "size_bytes" to sizeBytes,
            "size_mb" to sizeBytes.toDouble() / 1_048_576,
            "error_type" to error::class.java.simpleName
        )
    )

    private fun capture(event: String, properties: Map<String, Any>) {
        if (initialized) PostHog.capture(event = event, properties = properties)
    }
}

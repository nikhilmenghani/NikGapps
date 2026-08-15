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

    fun zipCreationSucceeded(sizeBytes: Long, conflictResolution: String) = capture(
        "zip_creation_succeeded",
        mapOf(
            "size_mb" to sizeBytes / 1_048_576,
            "conflict_resolution" to conflictResolution
        )
    )

    fun zipCreationFailed(error: Throwable) = capture(
        "zip_creation_failed",
        mapOf("error_type" to error::class.java.simpleName)
    )

    private fun capture(event: String, properties: Map<String, Any>) {
        if (initialized) PostHog.capture(event = event, properties = properties)
    }
}

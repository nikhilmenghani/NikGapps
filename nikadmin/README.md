# NikAdmin

NikAdmin is the separate, installable analytics dashboard for NikGapps. It reads
`zip_creation_succeeded` activity from PostHog; the NikGapps app continues to send
anonymous product events with its project ingestion key.

Build and install the debug app with:

```powershell
.\gradlew.bat :nikadmin:assembleDebug
adb install -r nikadmin\build\outputs\apk\debug\nikadmin-debug.apk
```

The debug build reads the existing `POSTHOG_PERSONAL_API_KEY`,
`POSTHOG_PROJECT_ID`, and optional `POSTHOG_API_HOST` Gradle properties or
environment variables. Use a PostHog personal API key with access to the
project's query endpoint. Do not commit these credentials. A distributable
release build intentionally contains no personal API key or project ID and
therefore shows setup instructions instead of live analytics.

NikAdmin has its own private analytics cache. Existing cached history from the
NikGapps app is not copied between Android app sandboxes; NikAdmin fetches the
history again on first launch.

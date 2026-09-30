# Native PostHog analytics dashboard

The Analytics tab is a native Compose view of `zip_creation_succeeded` activity. It uses the PostHog HogQL Query API only in local debug builds.

Set these values in the user-level Gradle properties file (`~/.gradle/gradle.properties`) or as environment variables:

```properties
POSTHOG_PERSONAL_API_KEY=phx_your_personal_api_key
POSTHOG_PROJECT_ID=12345
POSTHOG_API_HOST=https://us.posthog.com
```

Use `https://eu.posthog.com` for an EU-hosted PostHog project. `POSTHOG_API_HOST` defaults to `https://us.posthog.com`.

The Analytics navigation tab is added only when the build is debuggable and both the personal API key and project ID are present. Release builds explicitly replace both values with empty strings, so the tab is absent and the read credential is not embedded. Do not distribute a debug APK containing the personal API key.

Event ingestion continues to use `POSTHOG_API_KEY` and `POSTHOG_HOST` independently.

## Incremental history cache

The dashboard stores event rows and its successful sync checkpoint in the app-private `noBackupFilesDir/analytics` directory. The cache is separated by PostHog host and project ID, excludes read credentials, and survives app restarts and normal app upgrades. Uninstalling or clearing app data removes it.

Opening Analytics displays cached history before refreshing. The first sync fetches all available `zip_creation_succeeded` events in 500-row pages. Later syncs dynamically query from seven days before the last successful sync through the start of the current refresh. Rows are merged by UUID, so overlap does not duplicate builds, packages, or user totals. Keyset pagination uses timestamp and UUID, preserving fractional timestamp precision.

The checkpoint advances only after every page succeeds and the merged snapshot is atomically saved. Network failures, incomplete responses, and rate limits retain the previous snapshot and checkpoint. A retry fetches the same missing interval. HTTP 429 honors Retry-After (or a one-minute fallback) within the current screen session.

Once every 30 days, a full-history reconciliation also adds delayed offline uploads whose event timestamps predate the seven-day overlap. Such uploads may remain absent until that reconciliation. Counts and filters are calculated from all cached rows, not just the newest page. This is an append-oriented local history: server-side deletions are not automatically removed from it.

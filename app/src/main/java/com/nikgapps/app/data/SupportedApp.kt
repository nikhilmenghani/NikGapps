package com.nikgapps.app.data

data class SupportedApp(
    val id: String,
    val name: String,
    val packageName: String
)

object SupportedApps {
    val all = listOf(
        SupportedApp("gms_core", "Google Play services", "com.google.android.gms"),
        SupportedApp("play_store", "Google Play Store", "com.android.vending"),
        SupportedApp(
            "services_framework",
            "Google Services Framework",
            "com.google.android.gsf"
        ),
        SupportedApp(
            "contacts_sync",
            "Google Contacts Sync",
            "com.google.android.syncadapters.contacts"
        ),
        SupportedApp(
            "calendar_sync",
            "Google Calendar Sync",
            "com.google.android.syncadapters.calendar"
        ),
        SupportedApp("google_app", "Google app", "com.google.android.googlequicksearchbox"),
        SupportedApp("gmail", "Gmail", "com.google.android.gm"),
        SupportedApp("maps", "Google Maps", "com.google.android.apps.maps"),
        SupportedApp("photos", "Google Photos", "com.google.android.apps.photos"),
        SupportedApp("youtube", "YouTube", "com.google.android.youtube")
    )
}

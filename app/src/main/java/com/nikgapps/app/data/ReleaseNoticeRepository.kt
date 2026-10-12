package com.nikgapps.app.data

import android.content.Context

class ReleaseNoticeRepository(context: Context) {
    private val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
    private val preferences = context.getSharedPreferences("registry_release_notices", Context.MODE_PRIVATE)
    fun dismissed(id: String) = id in preferences.getStringSet("dismissed", emptySet()).orEmpty()
    fun dismiss(id: String) {
        preferences.edit().putStringSet("dismissed", preferences.getStringSet("dismissed", emptySet()).orEmpty() + id).apply()
    }
    fun notified(id: String) = id in preferences.getStringSet("notified", emptySet()).orEmpty()
    fun acknowledge(id: String) {
        dismiss(id)
        markNotified(id)
        notificationManager.cancel(id.hashCode())
    }
    fun notificationBaselineEstablished() = preferences.getBoolean("notification_baseline", false)
    fun establishNotificationBaseline() { preferences.edit().putBoolean("notification_baseline", true).apply() }
    fun markNotified(id: String) {
        preferences.edit().putStringSet("notified", preferences.getStringSet("notified", emptySet()).orEmpty() + id).apply()
    }
}

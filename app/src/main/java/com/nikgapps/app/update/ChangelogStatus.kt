package com.nikgapps.app.update

internal enum class ChangelogStatus { INSTALLED, UPCOMING, RELEASED }

internal fun changelogStatus(version: String, installed: String, published: String?): ChangelogStatus {
    if (ChangelogRepository.compareVersions(version, installed) == 0) return ChangelogStatus.INSTALLED
    if (published.isNullOrBlank() || published == "Unknown") return ChangelogStatus.RELEASED
    return if (ChangelogRepository.compareVersions(version, published) > 0 &&
        ChangelogRepository.compareVersions(version, installed) > 0) ChangelogStatus.UPCOMING else ChangelogStatus.RELEASED
}

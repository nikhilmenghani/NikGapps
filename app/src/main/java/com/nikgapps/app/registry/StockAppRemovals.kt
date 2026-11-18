package com.nikgapps.app.registry

import com.nikgapps.app.data.BuildProject

/** Apply selection and stock preference in one project update, preserving its other selections. */
fun BuildProject.withStockAppPreference(packageId: String, appSetId: String, keep: Boolean): BuildProject {
    val selected = selectProjectPackages(this, setOf(packageId), true, appSetId)
    return selected.copy(keepAospCounterparts = if (keep) selected.keepAospCounterparts + packageId
        else selected.keepAospCounterparts - packageId)
}

/** Removal rules for the exact package version the project will build. */
fun CatalogPackage.stockAppRemovals(project: BuildProject, release: CatalogRelease? = null): List<String> {
    val versionKey = release?.packages?.get(id)
        ?: channels[project.channelOverrides[id] ?: project.defaultChannel]
        ?: return emptyList()
    return versions[versionKey]?.install?.removeFiles.orEmpty()
        .filter(String::isNotBlank).distinct()
}

/** Names checked by the debloater, including the app directory for a full APK path. */
internal fun stockAppConflictNames(target: String): Set<String> {
    val segments = target.trimEnd('/').split('/').filter(String::isNotBlank)
    val appDirectory = segments.indexOfFirst { it == "app" || it == "priv-app" }
        .takeIf { it >= 0 }?.let { segments.getOrNull(it + 1) }
    return setOfNotNull(target.takeIf(String::isNotBlank), appDirectory,
        segments.lastOrNull()?.removeSuffix(".apk"))
}

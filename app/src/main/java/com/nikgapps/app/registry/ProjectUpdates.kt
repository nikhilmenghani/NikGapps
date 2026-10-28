package com.nikgapps.app.registry

import com.nikgapps.app.data.BuildProject

enum class ProjectUpdateKind { CURRENT, AVAILABLE, INCOMPATIBLE }
data class ProjectUpdate(val kind: ProjectUpdateKind, val releaseId: String?, val message: String)

/** Announce an update only when a saved, compatible project can actually use it. */
fun releaseAvailableToProjects(release: ReleaseSummary, projects: List<BuildProject>,
    updates: Map<String, ProjectUpdate>): Boolean = projects.any { project ->
    catalogAndroidVersion(project.androidVersion.displayName) == release.androidVersion &&
        project.defaultChannel == release.channel && project.architecture.value == release.architecture &&
        updates[project.id]?.let { it.kind == ProjectUpdateKind.AVAILABLE && it.releaseId == release.id } == true
}

fun releaseKnownToProjects(release: ReleaseSummary, projects: List<BuildProject>): Boolean = projects.any {
    catalogAndroidVersion(it.androidVersion.displayName) == release.androidVersion &&
        it.defaultChannel == release.channel && it.architecture.value == release.architecture &&
        (it.lastBuiltReleaseId ?: it.createdReleaseId) == release.id
}

fun dismissProjectsThroughRelease(projects: List<BuildProject>, target: ReleaseSummary,
    metadataByProject: Map<String, RegistryMetadata>): List<BuildProject> = projects.map { project ->
    val metadata = metadataByProject[project.id]
    val pending = metadata?.release
    val eligible = pending != null && pending.androidVersion == target.androidVersion &&
        pending.channel == target.channel && pending.architecture == target.architecture &&
        projectUpdate(project, metadata).kind == ProjectUpdateKind.AVAILABLE && runCatching {
            java.time.Instant.parse(pending.createdAt) <= java.time.Instant.parse(target.createdAt)
        }.getOrDefault(false)
    if (eligible) project.copy(dismissedReleaseId = target.id) else project
}

fun projectUpdate(project: BuildProject, metadata: RegistryMetadata): ProjectUpdate {
    val release = metadata.release
    if (release != null && (release.androidVersion != catalogAndroidVersion(project.androidVersion.displayName) ||
        release.channel != project.defaultChannel || release.architecture != project.architecture.value))
        return ProjectUpdate(ProjectUpdateKind.CURRENT, null, "")
    val config = metadata.builderAssets[RegistryZipAssembler.CONFIG_TEMPLATE]
    val missing = unavailableProjectPackages(project, metadata)
    val changedOwners = project.selectedPackageAppSets.any { (id, owner) ->
        metadata.appSets.appSets.none { it.id == owner && id in it.packages }
    }
    val newerConfig = (config?.configVersion?.toIntOrNull() ?: 0) > (project.configVersion?.toIntOrNull() ?: Int.MAX_VALUE)
    val changedDefinitions = newerConfig && project.selectedAppIds.isNotEmpty() && (project.packageDefinitions.isEmpty() ||
        project.selectedAppIds.any { project.packageDefinitions[it] != config?.packageDefinitions?.get(it) })
    if (missing.isNotEmpty() || changedOwners || changedDefinitions ||
        config?.configSettings?.let { keys -> project.installerSettings.keys.any { it !in keys } } == true) return ProjectUpdate(
        ProjectUpdateKind.INCOMPATIBLE, release?.id,
        "This project's configuration is incompatible with the latest NikGapps definitions. Export your config, update the affected entries and config version, then import it to create a new project.")
    val baseline = project.lastBuiltReleaseId ?: project.createdReleaseId
    val baselineDate = metadata.releaseIndex?.releases?.firstOrNull { it.id == baseline }?.createdAt
    val newRelease = release != null && release.id != baseline && runCatching {
        val previous = baselineDate?.let(java.time.Instant::parse)?.toEpochMilli() ?: project.createdAt
        java.time.Instant.parse(release.createdAt).toEpochMilli() > previous
    }.getOrDefault(false)
    val dismissedDate = metadata.releaseIndex?.releases?.firstOrNull { it.id == project.dismissedReleaseId }?.createdAt
    val dismissed = project.dismissedReleaseId != null && (project.dismissedReleaseId == release?.id ||
        (release != null && dismissedDate != null && runCatching {
            java.time.Instant.parse(release.createdAt) <= java.time.Instant.parse(dismissedDate)
        }.getOrDefault(false)))
    val update = (newRelease || newerConfig) && !dismissed
    return ProjectUpdate(if (update) ProjectUpdateKind.AVAILABLE else ProjectUpdateKind.CURRENT,
        release?.id, if (update) "A newer NikGapps release is available with updated packages and fixes." else "")
}

fun BuildProject.withCurrentConfig(metadata: RegistryMetadata): BuildProject {
    val template = metadata.builderAssets[RegistryZipAssembler.CONFIG_TEMPLATE]
    return copy(configVersion = template?.configVersion ?: configVersion,
        packageDefinitions = template?.packageDefinitions.orEmpty().filterKeys { it in selectedAppIds })
}

/** Platform edits establish a fresh baseline; old build/config receipts belong to the old target. */
fun BuildProject.rebasePlatform(metadata: RegistryMetadata): BuildProject =
    copy(createdReleaseId = metadata.release?.id, lastBuiltReleaseId = null,
        dismissedReleaseId = null, configSnapshot = null).withCurrentConfig(metadata)

fun BuildProject.afterSuccessfulBuild(metadata: RegistryMetadata): BuildProject = withCurrentConfig(metadata).copy(
    lastBuiltReleaseId = metadata.release?.id, dismissedReleaseId = dismissedReleaseId?.takeUnless { it == metadata.release?.id })

fun recoveryProjectConfig(project: BuildProject, metadata: RegistryMetadata): String {
    project.configSnapshot?.let { return it }
    val groups = project.selectedAppIds.groupBy { id -> project.selectedPackageAppSets[id] ?: project.selectedAppSetId }
    return buildString {
        append("# Exported project configuration. Review incompatible entries before importing.\n")
        append("AndroidVersion=${catalogAndroidVersion(project.androidVersion.displayName)}\n")
        append("Version=${project.configVersion ?: metadata.builderAssets[RegistryZipAssembler.CONFIG_TEMPLATE]?.configVersion ?: "40"}\n")
        append("UseZipConfig=1\n\n# Following are the packages with default configuration\n")
        groups.forEach { (owner, ids) ->
            val appSet = metadata.appSets.appSets.firstOrNull { it.id == owner }
            append("${appSet?.name ?: owner}=1\n")
            ids.forEach { id ->
                val name = appSet?.legacyPackageNames?.get(id) ?: metadata.catalog.packages.firstOrNull { it.id == id }?.name ?: id
                append(">>$name=${if (id in project.keepAospCounterparts) 2 else 1}\n")
            }
        }
    }
}

package com.nikgapps.app.registry

import com.nikgapps.app.data.BuildProject

internal data class BackupRestorePlan(val compatible: List<BuildProject>, val outdated: List<BuildProject>)
internal data class BackupTarget(val android: String, val channel: String, val architecture: String)
internal fun BuildProject.backupTarget() = BackupTarget(catalogAndroidVersion(androidVersion.displayName), defaultChannel, architecture.value)

internal fun planBackupRestore(projects: List<BuildProject>, metadata: Map<BackupTarget, RegistryMetadata>): BackupRestorePlan {
    val compatible = mutableListOf<BuildProject>()
    val outdated = mutableListOf<BuildProject>()
    projects.forEach { project ->
        val loaded = metadata[project.backupTarget()] ?: error("Could not verify all backup projects")
        check(!loaded.fromCache) { "Connect to the internet to verify backup compatibility. No projects were changed." }
        val target = project.backupTarget()
        val release = loaded.release
        val supported = if (release != null) release.androidVersion == target.android &&
            release.channel == target.channel && release.architecture == target.architecture
        else loaded.releaseIndex == null && loaded.catalog.androidVersion == target.android && loaded.catalog.architecture == target.architecture
        val invalid = !supported || unavailableProjectPackages(project, loaded).isNotEmpty() ||
            projectUpdate(project, loaded).kind == ProjectUpdateKind.INCOMPATIBLE
        val canResolve = !invalid && runCatching {
            val owners = project.selectedAppIds.associateWith { id -> project.selectedPackageAppSets[id]
                ?: loaded.appSets.appSets.firstOrNull { id in it.packages }?.id ?: error("No AppSet owns $id") }
            CatalogResolver(loaded.catalog, loaded.appSets, loaded.release).resolveAcrossAppSets(
                owners, ReleaseChannel.valueOf(project.defaultChannel.uppercase()),
                project.channelOverrides.mapValues { ReleaseChannel.valueOf(it.value.uppercase()) },
                project.androidVersion.apiLevel, project.architecture.value)
        }.isSuccess
        if (canResolve) compatible += project else outdated += project
    }
    return BackupRestorePlan(compatible, outdated)
}

internal suspend fun reviewBackupProjects(projects: List<BuildProject>, repository: CatalogRepository): BackupRestorePlan {
    val metadata = mutableMapOf<BackupTarget, RegistryMetadata>()
    projects.map { it.backupTarget() }.distinct().forEach { target ->
        metadata[target] = repository.load(target.android, target.channel, target.architecture, forceRefresh = true)
    }
    return planBackupRestore(projects, metadata)
}

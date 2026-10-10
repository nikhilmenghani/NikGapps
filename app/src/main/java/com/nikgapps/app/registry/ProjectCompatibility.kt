package com.nikgapps.app.registry

import com.nikgapps.app.data.BuildProject
import com.nikgapps.app.data.MAX_PROJECT_NAME_LENGTH

/** Current release AppSets, not historical catalog entries, define what can be selected. */
fun unavailableProjectPackages(project: BuildProject, metadata: RegistryMetadata): Set<String> {
    val available = metadata.appSets.appSets.flatMap { appSet ->
        metadata.catalog.publicPackages(appSet).map { it.id }
    }.toSet()
    return project.selectedAppIds - available
}

fun duplicateCurrentProject(project: BuildProject, metadata: RegistryMetadata, maxNameLength: Int = 20): BuildProject {
    val selected = project.selectedAppIds - unavailableProjectPackages(project, metadata)
    val owners = project.selectedPackageAppSets.filterKeys { it in selected }
    val sources = project.appSources.filterKeys { it in selected }
    val overrides = project.channelOverrides.filterKeys { it in selected }
    return BuildProject(
        name = "${project.name} copy".take(maxNameLength.coerceIn(1, MAX_PROJECT_NAME_LENGTH)),
        androidVersion = project.androidVersion,
        architecture = project.architecture,
        selectedAppSetId = project.selectedAppSetId,
        selectedPackageAppSets = owners,
        defaultChannel = project.defaultChannel,
        channelOverrides = overrides,
        selectedAppIds = selected,
        appSources = sources,
        keepAospCounterparts = project.keepAospCounterparts.intersect(selected)
    )
}

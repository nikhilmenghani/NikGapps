package com.nikgapps.app.registry

import com.nikgapps.app.data.BuildProject

/** Map only the known Android 17 v42 replacement, never old/pinned releases. */
fun BuildProject.withAndroid17GmsCore(metadata: RegistryMetadata): BuildProject {
    val template = metadata.builderAssets[RegistryZipAssembler.CONFIG_TEMPLATE] ?: return this
    if (catalogAndroidVersion(androidVersion.displayName) != "17" || metadata.release?.androidVersion != "17" ||
        (template?.configVersion?.toIntOrNull() ?: 0) < 42 || "gms_core" !in selectedAppIds) return this
    val owner = selectedPackageAppSets["gms_core"] ?: selectedAppSetId
    val replacement = when (owner) {
        "core" -> "gms_core_android17"
        "core_go" -> "gms_core_go_android17"
        else -> return this
    }
    val set = metadata.appSets.appSets.firstOrNull { it.id == owner } ?: return this
    if (replacement !in set.packages || metadata.catalog.publicPackages(set).none { it.id == replacement }) return this
    fun <T> remap(values: Map<String, T>) = values.mapKeys { (id, _) -> if (id == "gms_core") replacement else id }
    return copy(
        selectedAppIds = selectedAppIds.map { if (it == "gms_core") replacement else it }.toSet(),
        selectedPackageAppSets = remap(selectedPackageAppSets) + (replacement to owner),
        channelOverrides = remap(channelOverrides), appSources = remap(appSources),
        keepAospCounterparts = keepAospCounterparts.map { if (it == "gms_core") replacement else it }.toSet(),
        packageDefinitions = (packageDefinitions - "gms_core") + template.packageDefinitions.filterKeys { it == replacement },
        configSnapshot = null)
}

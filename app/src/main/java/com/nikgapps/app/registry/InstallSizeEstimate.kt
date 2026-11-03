package com.nikgapps.app.registry

import com.nikgapps.app.data.BuildProject
import java.util.Locale

/** Installer payloadSize is KiB (matching df -k); file metadata sizes are bytes. */
internal fun PackageVersion.installBytes(): Long? = install?.payloadSize?.takeIf { it in 0..(Long.MAX_VALUE / 1024) }?.times(1024)
    ?: files.takeIf { it.isNotEmpty() && it.all { file -> file.size >= 0 } }?.sumOf { it.size }

internal data class InstallSizeEstimate(val knownBytes: Long, val partitions: Map<String, Long>,
    val missingPackages: Int) {
    val complete get() = missingPackages == 0
}

internal fun estimateInstallSize(packages: List<ResolvedPackage>): InstallSizeEstimate {
    val unique = packages.distinctBy { it.catalogPackage.id }
    val known = unique.mapNotNull { pkg -> pkg.version.installBytes()?.let { pkg.version.defaultPartition to it } }
    return InstallSizeEstimate(known.sumOf { it.second },
        known.groupBy({ it.first }, { it.second }).toSortedMap().mapValues { it.value.sum() },
        unique.size - known.size)
}

internal fun projectInstallSize(project: BuildProject, metadata: RegistryMetadata): InstallSizeEstimate {
    val selections = project.selectedAppIds.associateWith { id ->
        project.selectedPackageAppSets[id] ?: metadata.appSets.appSets.firstOrNull { id in it.packages }?.id
            ?: error("No AppSet owns '$id'")
    }
    val packages = CatalogResolver(metadata.catalog, metadata.appSets, metadata.release).resolveAcrossAppSets(
        selections, ReleaseChannel.valueOf(project.defaultChannel.uppercase()),
        project.channelOverrides.mapValues { ReleaseChannel.valueOf(it.value.uppercase()) },
        project.androidVersion.apiLevel, project.architecture.value).packages
    return estimateInstallSize(packages)
}

internal fun formatInstallSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> String.format(Locale.getDefault(), "%.2f GiB", bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> String.format(Locale.getDefault(), "%.1f MiB", bytes / 1_048_576.0)
    bytes >= 1024 -> String.format(Locale.getDefault(), "%.1f KiB", bytes / 1024.0)
    else -> "$bytes B"
}

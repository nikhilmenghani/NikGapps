package com.nikgapps.app.registry

import com.nikgapps.app.data.BuildProject

object NikGappsConfigExporter {
    fun forProject(template: String, project: BuildProject, metadata: RegistryMetadata): String {
        require(unavailableProjectPackages(project, metadata).isEmpty()) { "Project has unavailable packages" }
        val owners = project.selectedAppIds.associateWith { id ->
            project.selectedPackageAppSets[id]
                ?: metadata.appSets.appSets.firstOrNull { id in it.packages }?.id
                ?: error("No AppSet owns '$id'")
        }
        val resolution = CatalogResolver(metadata.catalog, metadata.appSets, metadata.release).resolveAcrossAppSets(
            owners, ReleaseChannel.valueOf(project.defaultChannel.uppercase()),
            project.channelOverrides.mapValues { ReleaseChannel.valueOf(it.value.uppercase()) },
            project.androidVersion.apiLevel, project.architecture.value)
        val selected = linkedMapOf<String, MutableSet<String>>()
        resolution.packages.forEach { pkg ->
            val set = resolution.packageAppSets.getValue(pkg.catalogPackage.id)
            selected.getOrPut(set.name) { linkedSetOf() } +=
                set.legacyPackageNames[pkg.catalogPackage.id] ?: pkg.catalogPackage.name
        }
        return render(template, project.androidVersion.displayName, selected)
    }

    fun render(template: String, androidVersion: String, selected: Map<String, Set<String>>): String {
        require(template.contains("# Following are the packages")) { "Config template has no package section" }
        var packageSection = false
        var group: String? = null
        var androidLine = false
        var useZipConfigLine = false
        val lines = template.lineSequence().map { original ->
            val line = original.trimEnd('\r')
            when {
                line.startsWith("UseZipConfig=") -> {
                    useZipConfigLine = true
                    "UseZipConfig=1"
                }
                line.startsWith("AndroidVersion=") -> {
                    androidLine = true
                    "AndroidVersion=${catalogAndroidVersion(androidVersion)}"
                }
                line.startsWith("# Following are the packages") -> {
                    packageSection = true
                    line
                }
                packageSection && !line.trimStart().startsWith('#') && '=' in line -> {
                    val key = line.substringBefore('=').trim()
                    val enabled = if (key.startsWith(">>")) {
                        key.removePrefix(">>") in selected[group].orEmpty()
                    } else {
                        group = key
                        selected[key].orEmpty().isNotEmpty()
                    }
                    "$key=${if (enabled) 1 else 0}"
                }
                else -> line
            }
        }.toList()
        val headers = buildList {
            if (!androidLine) add("AndroidVersion=${catalogAndroidVersion(androidVersion)}")
            if (!useZipConfigLine) add("UseZipConfig=1")
        }
        return (headers + lines)
            .joinToString("\n").trimEnd() + "\n"
    }
}

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
        val kept = linkedMapOf<String, MutableSet<String>>()
        resolution.packages.forEach { pkg ->
            val set = resolution.packageAppSets.getValue(pkg.catalogPackage.id)
            selected.getOrPut(set.name) { linkedSetOf() } +=
                set.legacyPackageNames[pkg.catalogPackage.id] ?: pkg.catalogPackage.name
            if (pkg.catalogPackage.id in project.keepAospCounterparts)
                kept.getOrPut(set.name) { linkedSetOf() } +=
                    set.legacyPackageNames[pkg.catalogPackage.id] ?: pkg.catalogPackage.name
        }
        return render(template, project.androidVersion.displayName, selected, kept)
    }

    fun render(template: String, androidVersion: String, selected: Map<String, Set<String>>,
        keepAosp: Map<String, Set<String>> = emptyMap()): String {
        require(template.contains("# Following are the packages")) { "Config template has no package section" }
        var packageSection = false
        var group: String? = null
        var androidLine = false
        var useZipConfigLine = false
        val groupedHeaders = mutableSetOf<String>()
        var header: String? = null
        template.lineSequence().forEach { line ->
            if (!line.startsWith('#') && '=' in line) {
                val key = line.substringBefore('=').trim()
                if (key.startsWith(">>")) header?.let { groupedHeaders += it } else header = key
            }
        }
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
                    val keep = if (key.startsWith(">>")) key.removePrefix(">>") in keepAosp[group].orEmpty()
                        else key !in groupedHeaders && key in keepAosp[key].orEmpty()
                    "$key=${if (!enabled) 0 else if (keep) 2 else 1}"
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

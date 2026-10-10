package com.nikgapps.app.registry

import com.nikgapps.app.data.BuildProject

/** Only package selections are imported; project/build settings are left untouched. */
object NikGappsConfigImporter {
    private data class Entry(val group: String, val name: String, val value: Int)
    private data class Parsed(val headers: Map<String, String>, val entries: List<Entry>)

    private fun parse(text: String): Parsed {
        val headers = linkedMapOf<String, String>()
        val entries = mutableListOf<Entry>()
        var packageSection = false
        var group: String? = null
        text.removePrefix("\uFEFF").lineSequence().forEach { original ->
            val line = original.trim()
            if (line.startsWith("# Following are the packages")) packageSection = true
            if (line.isEmpty() || line.startsWith('#')) return@forEach
            require('=' in line) { "Invalid config line: $line" }
            val key = line.substringBefore('=').trim()
            val value = line.substringAfter('=').substringBefore('#').trim()
            if (!packageSection) {
                require(headers.put(key, value) == null) { "Duplicate config setting: $key" }
            } else {
                val nested = key.startsWith(">>")
                if (!nested) group = key
                val owner = group ?: error("Package has no AppSet: $key")
                val name = if (nested) key.removePrefix(">>").trim() else key
                val flag = value.toIntOrNull()
                require(flag != null && flag in 0..2) { "Unsupported package value for $key: $value (use 0, 1 or 2)" }
                val entryName = if (nested) ">>$name" else name
                require(entries.none { it.group == owner && it.name == entryName }) {
                    "Duplicate package entry: $key"
                }
                // Keep the prefix so a group and a child with the same name are distinct.
                entries += Entry(owner, entryName, flag)
            }
        }
        require(packageSection) { "Config has no package section" }
        return Parsed(headers, entries)
    }

    fun forProject(text: String, template: String, project: BuildProject, metadata: RegistryMetadata): BuildProject {
        val input = parse(text)
        val latest = parse(template)
        val version = latest.headers["Version"] ?: error("Published config has no version")
        require(input.headers["Version"] == version) { "Unsupported config version. Import version $version only." }
        val android = catalogAndroidVersion(project.androidVersion.displayName)
        require(input.headers["AndroidVersion"] == android) { "Config must target Android $android" }
        val supported = latest.entries.map { it.group to it.name }.toSet()
        val owners = linkedMapOf<String, String>()
        val kept = linkedSetOf<String>()
        val groups = input.entries.filter { !it.name.startsWith(">>") }.associate { it.group to it.value }
        val grouped = latest.entries.filter { it.name.startsWith(">>") }.map { it.group }.toSet()
        input.entries.forEach { entry ->
            require(entry.group to entry.name in supported) { "Unknown config package: ${entry.group}/${entry.name}" }
            if (entry.value == 0 || groups[entry.group] == 0) return@forEach
            if (!entry.name.startsWith(">>") && entry.group in grouped) return@forEach
            val set = metadata.appSets.appSets.firstOrNull { it.name == entry.group }
                ?: error("AppSet is unavailable: ${entry.group}")
            val legacyName = entry.name.removePrefix(">>")
            val id = (set.packages + set.resolvedPackages).distinct().firstOrNull { id ->
                (set.legacyPackageNames[id] ?: metadata.catalog.packages.firstOrNull { it.id == id }?.name) == legacyName
            } ?: error("Package is unavailable: ${entry.group}/$legacyName")
            require(owners[id] == null || owners[id] == set.id) { "Package selected in multiple AppSets: $legacyName" }
            owners[id] = set.id
            if (entry.value == 2 || groups[entry.group] == 2) kept += id
        }
        requireCompatibleAppSets(owners.values)
        val selectedOwners = owners.filter { (id, owner) ->
            id in metadata.appSets.appSets.first { it.id == owner }.packages &&
                metadata.catalog.packages.any { it.id == id && it.selectable && !it.internal }
        }
        // Validate availability, architecture, dependencies and current release before saving anything.
        val resolution = CatalogResolver(metadata.catalog, metadata.appSets, metadata.release).resolveAcrossAppSets(
            selectedOwners, ReleaseChannel.valueOf(project.defaultChannel.uppercase()),
            project.channelOverrides.mapValues { ReleaseChannel.valueOf(it.value.uppercase()) },
            project.androidVersion.apiLevel, project.architecture.value)
        require(owners.keys.all { id -> resolution.packages.any { it.catalogPackage.id == id } }) {
            "Config selects a dependency without its parent package"
        }
        return project.copy(selectedAppIds = selectedOwners.keys.toSet(), selectedPackageAppSets = selectedOwners,
            keepAospCounterparts = kept, appSources = project.appSources.filterKeys { it in selectedOwners })
    }
}

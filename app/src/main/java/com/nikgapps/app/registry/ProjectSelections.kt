package com.nikgapps.app.registry

import com.nikgapps.app.data.BuildProject

fun requireCompatibleAppSets(ids: Collection<String>) {
    AppSetSelectionRules.exclusiveGroups.forEach { rule ->
        require(ids.toSet().intersect(rule.members).size <= 1) { rule.message }
    }
}

fun isSelectedInAppSet(project: BuildProject, packageId: String, appSetId: String): Boolean =
    packageId in project.selectedAppIds && project.selectedPackageAppSets[packageId] == appSetId

fun selectProjectPackages(project: BuildProject, ids: Set<String>, enabled: Boolean,
    appSetId: String? = null, defaultOwners: Map<String, String> = emptyMap(),
    useDefaultChoices: Boolean = false): BuildProject {
    val affected = if (!enabled && appSetId != null)
        ids.filterTo(linkedSetOf()) { isSelectedInAppSet(project, it, appSetId) } else ids
    val owners = project.selectedPackageAppSets.toMutableMap()
    affected.forEach { id ->
        if (!enabled) owners.remove(id)
        else if (appSetId != null) owners[id] = appSetId
        else if (useDefaultChoices && id in defaultOwners) owners[id] = defaultOwners.getValue(id)
        else owners.putIfAbsent(id, defaultOwners[id] ?: project.selectedAppSetId)
    }
    val selected = (if (enabled) project.selectedAppIds + affected else project.selectedAppIds - affected).toMutableSet()
    if (enabled) AppSetSelectionRules.exclusiveGroups.forEach { rule ->
        val group = rule.members
        val activated = affected.mapNotNull { owners[it] }.filter { it in group }.toSet()
        if (activated.isNotEmpty()) {
            val existing = project.selectedAppIds.mapNotNull { project.selectedPackageAppSets[it] }
                .filter { it in group }.toSet()
            val preferred = rule.defaultMember.takeIf { useDefaultChoices && it in activated }
                ?: appSetId?.takeIf { it in group }
                ?: activated.singleOrNull() ?: existing.singleOrNull() ?: rule.defaultMember
            val removed = selected.filter { owners[it] in group && owners[it] != preferred }
            selected.removeAll(removed.toSet())
            removed.forEach { owners.remove(it) }
        }
    }
    return project.copy(
        selectedAppSetId = appSetId ?: project.selectedAppSetId,
        selectedAppIds = selected,
        selectedPackageAppSets = owners
    )
}

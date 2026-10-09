package com.nikgapps.app.registry

data class ExclusiveAppSetRule(val members: Set<String>, val defaultMember: String, val message: String) {
    init { require(defaultMember in members) }
}

/** Add future mutually exclusive AppSets and their Select-all default here. */
object AppSetSelectionRules {
    val exclusiveGroups = listOf(
        ExclusiveAppSetRule(setOf("core", "core_go"), "core", "Choose either Core or CoreGo before building"),
        ExclusiveAppSetRule(setOf("setup_wizard", "pixel_setup_wizard"), "setup_wizard",
            "Choose either SetupWizard or PixelSetupWizard before building")
    )
}

fun defaultOwnersForSelectAll(memberships: Map<String, List<CatalogAppSet>>): Map<String, String> {
    val available = memberships.values.flatten().map { it.id }.toSet()
    return memberships.mapNotNull { (id, sets) ->
        val allowed = sets.filter { set -> AppSetSelectionRules.exclusiveGroups.none { rule ->
            set.id in rule.members && set.id != rule.defaultMember && rule.defaultMember in available
        } }
        allowed.firstOrNull()?.let { id to it.id }
    }.toMap()
}

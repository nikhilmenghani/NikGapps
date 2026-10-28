package com.nikgapps.app.registry

import com.nikgapps.app.data.BuildProject

/** Classify once before revealing cards; refreshed status must not move visible projects. */
fun initialProjectDisplayOrder(projects: List<BuildProject>, unavailable: Map<String, Set<String>>,
    updates: Map<String, ProjectUpdate>): List<String> = projects.sortedBy {
    unavailable[it.id]?.isNotEmpty() == true || updates[it.id]?.kind == ProjectUpdateKind.INCOMPATIBLE
}.map { it.id }

/** Keep existing positions, drop deleted projects and append newly created ones. */
fun projectsInDisplayOrder(projects: List<BuildProject>, order: List<String>): List<BuildProject> {
    val byId = projects.associateBy { it.id }
    val known = order.toSet()
    return order.distinct().mapNotNull(byId::get) + projects.filter { it.id !in known }
}

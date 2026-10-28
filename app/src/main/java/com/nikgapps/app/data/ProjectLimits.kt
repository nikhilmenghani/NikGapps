package com.nikgapps.app.data

const val REGULAR_PROJECT_LIMIT = 5

class ProjectLimitReachedException : IllegalStateException(
    "Regular accounts can keep up to $REGULAR_PROJECT_LIMIT projects. Delete a project or upgrade to Elite to create more.")

fun canCreateProject(existingCount: Int, elite: Boolean): Boolean = elite || existingCount < REGULAR_PROJECT_LIMIT

fun requireProjectCapacity(existingCount: Int, elite: Boolean) {
    if (!canCreateProject(existingCount, elite)) throw ProjectLimitReachedException()
}

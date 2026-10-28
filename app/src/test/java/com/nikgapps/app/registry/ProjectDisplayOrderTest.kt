package com.nikgapps.app.registry

import com.nikgapps.app.data.*
import org.junit.Assert.*
import org.junit.Test

class ProjectDisplayOrderTest {
    private fun project(name: String) = BuildProject(id = name, name = name,
        androidVersion = AndroidVersion.ANDROID_17, architecture = Architecture.ARM64)

    @Test fun initialClassificationMovesOutdatedProjectsOnceWithoutRenaming() {
        val projects = listOf(project("a17"), project("core"), project("oldConfig"))
        val order = initialProjectDisplayOrder(projects, mapOf("a17" to setOf("Books")),
            mapOf("oldConfig" to ProjectUpdate(ProjectUpdateKind.INCOMPATIBLE, null, "Old definition")))
        assertEquals(listOf("core", "a17", "oldConfig"), order)
        assertEquals(listOf("core", "a17", "oldConfig"), projectsInDisplayOrder(projects, order).map { it.name })
    }

    @Test fun refreshesAndResumesKeepVisiblePositions() {
        val projects = listOf(project("a17"), project("core"))
        val frozen = initialProjectDisplayOrder(projects, mapOf("a17" to setOf("Books")), emptyMap())
        repeat(3) {
            assertEquals(listOf("core", "a17"), projectsInDisplayOrder(projects.map { it.copy() }, frozen).map { it.id })
        }
        // Even if fresh metadata would produce another order, the visible session uses its frozen IDs.
        assertEquals(listOf("a17", "core"), initialProjectDisplayOrder(projects, emptyMap(), emptyMap()))
        assertEquals(listOf("core", "a17"), projectsInDisplayOrder(projects, frozen).map { it.id })
    }

    @Test fun offlineFallbackAndProjectChangesAreStable() {
        val a = project("a"); val b = project("b"); val c = project("c")
        val order = initialProjectDisplayOrder(listOf(a, b), emptyMap(), emptyMap())
        assertEquals(listOf("a", "b"), order)
        assertEquals(listOf("b", "c"), projectsInDisplayOrder(listOf(c, b), order).map { it.id })
        assertTrue(projectsInDisplayOrder(emptyList(), order).isEmpty())
    }
}

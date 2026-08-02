package com.nikgapps.app.registry

import org.junit.Assert.*
import org.junit.Test

class CatalogResolverTest {
    private fun resolver(catalog: String = RegistryTestFixtures.catalog()) = CatalogResolver(
        CatalogParser.parseCatalog(catalog), CatalogParser.parseAppSets(RegistryTestFixtures.appSets()))
    @Test fun parsesCatalogAndAppSets() { assertEquals(4, CatalogParser.parseCatalog(RegistryTestFixtures.catalog()).packages.size); assertEquals(2, CatalogParser.parseAppSets(RegistryTestFixtures.appSets()).appSets.size) }
    @Test fun resolvesAllChannelsAndOverride() {
        ReleaseChannel.entries.forEach { channel -> assertEquals(channel.wireName, resolver().resolve("core", setOf("gms_core"), channel, emptyMap(), 35, "arm64-v8a").single().version.versionName) }
        assertEquals("canary", resolver().resolve("core", setOf("gms_core"), ReleaseChannel.STABLE,
            mapOf("gms_core" to ReleaseChannel.CANARY), 35, "arm64-v8a").single().version.versionName)
    }
    @Test fun coreAndCoreGoDependenciesAreConditional() {
        val deps = "{\"id\":\"support_common\"},{\"id\":\"support_standard\",\"when\":{\"appSet\":\"core\"}},{\"id\":\"support_go\",\"when\":{\"appSet\":\"core_go\"}}"
        assertEquals(listOf("support_common", "support_standard", "gms_core"), resolver(RegistryTestFixtures.catalog(deps)).resolve("core", setOf("gms_core"), ReleaseChannel.STABLE, emptyMap(), 35, "arm64-v8a").map { it.catalogPackage.id })
        assertEquals(listOf("support_common", "support_go", "gms_core"), resolver(RegistryTestFixtures.catalog(deps)).resolve("core_go", setOf("gms_core"), ReleaseChannel.STABLE, emptyMap(), 35, "arm64-v8a").map { it.catalogPackage.id })
    }
    @Test fun checksApiAndArchitectureButNotTargetApi() {
        resolver().resolve("core", setOf("gms_core"), ReleaseChannel.STABLE, emptyMap(), 35, "arm64-v8a")
        assertThrows(RegistryException.UnsupportedApi::class.java) { resolver().resolve("core", setOf("gms_core"), ReleaseChannel.STABLE, emptyMap(), 28, "arm64-v8a") }
        assertThrows(RegistryException.UnsupportedArchitecture::class.java) { resolver().resolve("core", setOf("gms_core"), ReleaseChannel.STABLE, emptyMap(), 35, "x86") }
    }
    @Test fun rejectsCycleAndIsDeterministic() {
        assertThrows(RegistryException.DependencyCycle::class.java) { resolver(RegistryTestFixtures.catalog(cycle = true)).resolve("core", setOf("gms_core"), ReleaseChannel.STABLE, emptyMap(), 35, "arm64-v8a") }
        val r = resolver().resolve("core", setOf("gms_core"), ReleaseChannel.STABLE, emptyMap(), 35, "arm64-v8a")
        assertEquals(r, resolver().resolve("core", setOf("gms_core"), ReleaseChannel.STABLE, emptyMap(), 35, "arm64-v8a"))
    }
}

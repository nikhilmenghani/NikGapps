package com.nikgapps.app.registry

import org.junit.Assert.*
import org.junit.Test

class InstallSizeEstimateTest {
    private val base = CatalogParser.parseCatalog(RegistryTestFixtures.catalog()).packages.first()
    private val version = base.versions.values.first()
    private fun payload(bytes: Long) = InstallMetadata("nikgapps-package-v1", "App", "App", bytes,
        emptyList(), emptyList(), emptyList(), false, "09")
    private fun resolved(id: String, size: Long?, partition: String = "product") = ResolvedPackage(
        base.copy(id = id), "test", version.copy(install = size?.let(::payload), files = emptyList(),
            defaultPartition = partition), ReleaseChannel.STABLE, id == "dependency")

    @Test fun usesInstallerPayloadInsteadOfCompressedArtifactSize() {
        val item = resolved("app", 12345)
        assertEquals(12345L * 1024, item.version.installBytes())
    }
    @Test fun includesHiddenDependenciesOnceAndSeparatesPartitions() {
        val dependency = resolved("dependency", 200, "system_ext")
        val estimate = estimateInstallSize(listOf(resolved("app", 100), dependency, dependency))
        assertEquals(300L * 1024, estimate.knownBytes)
        assertEquals(mapOf("product" to 100L * 1024, "system_ext" to 200L * 1024), estimate.partitions)
        assertTrue(estimate.complete)
    }
    @Test fun missingMetadataDoesNotPretendToBeZero() {
        val estimate = estimateInstallSize(listOf(resolved("app", 100), resolved("unknown", null)))
        assertEquals(100L * 1024, estimate.knownBytes)
        assertEquals(1, estimate.missingPackages)
        assertFalse(estimate.complete)
    }
    @Test fun gmsInstallerPayloadIsApproximately182MiBNot182KiB() {
        assertEquals(191299584L, resolved("gms_core", 186816).version.installBytes())
        assertTrue(resolved("gms_core", 186816).version.installBytes()!! > 180L * 1024 * 1024)
    }
    @Test fun fallsBackToUncompressedFileSizesWhenPayloadIsAbsent() {
        val value = version.copy(install = null, files = listOf(
            CatalogFile("a", null, null, "hash", 20, "primaryApk"),
            CatalogFile("b", null, null, "hash", 30, "other")))
        assertEquals(50L, value.installBytes())
    }
}

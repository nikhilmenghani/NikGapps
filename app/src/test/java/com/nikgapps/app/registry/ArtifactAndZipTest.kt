package com.nikgapps.app.registry

import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ArtifactAndZipTest {
    @Test fun sha256AndCacheReuse() = runBlocking {
        val dir = Files.createTempDirectory("registry-cache").toFile(); val bytes = "cached".toByteArray()
        val sha = RegistryTestFixtures.hash(bytes); val cache = File(dir, "nikgapps/packages/$sha.zip").apply { parentFile.mkdirs(); writeBytes(bytes) }
        val resolved = resolved(Artifact("https://127.0.0.1:1/no", sha, bytes.size.toLong()))
        assertEquals(cache, ArtifactDownloader(dir, maxAttempts = 1).obtain(resolved)); assertEquals(sha, ArtifactDownloader.sha256(cache))
    }
    @Test fun rejectsTraversal() {
        val dir = Files.createTempDirectory("bad-zip").toFile(); val zip = File(dir, "bad.zip")
        ZipOutputStream(zip.outputStream()).use { it.putNextEntry(ZipEntry("../evil")); it.write(1); it.closeEntry() }
        assertThrows(InvalidPackageZip::class.java) { PackageZipValidator().validate(zip, resolved()) }
    }
    @Test fun validatesAndAssemblesFakeCore() {
        val dir = Files.createTempDirectory("core-build").toFile(); val (zip, sha) = RegistryTestFixtures.artifact(dir)
        val resolved = resolved(Artifact(zip.toURI().toString(), sha, zip.length()))
        val descriptor = PackageZipValidator().validate(zip, resolved)
        val assets = RegistryZipAssembler.REQUIRED_ASSETS.associateWith { "asset".toByteArray() }.toMutableMap().apply {
            put(RegistryZipAssembler.CONFIG_TEMPLATE, "AndroidVersion=16\nGmsCore=1\n".toByteArray())
            put(RegistryZipAssembler.CUSTOMIZE_TEMPLATE, "DEBUG=true\n".toByteArray())
        }
        val set = CatalogParser.parseAppSets(RegistryTestFixtures.appSets()).appSets.first()
        val output = RegistryZipAssembler { assets }.build(dir, BuildRequest("16", 36, "arm64-v8a", set,
            ReleaseChannel.STABLE, emptyMap(), setOf("gms_core"), Instant.parse("2026-08-02T00:00:00Z")),
            listOf(ValidatedArtifact(resolved, zip, descriptor)))
        ZipFile(output).use { built ->
            assertNotNull(built.getEntry("AppSet/Core/GmsCore.zip")); assertNotNull(built.getEntry("nikgapps/build-manifest.json"))
            assertTrue(built.getInputStream(built.getEntry("afzc/nikgapps.config")).bufferedReader().readText().contains("GmsCore=1"))
            listOf("afzc/debloater.config", "changelog.yaml", "common/mtg_mount.sh", "customize.sh",
                "module.prop", "busybox", "creator.txt").forEach { assertNotNull(it, built.getEntry(it)) }
            assertNull(built.getEntry(RegistryZipAssembler.CONFIG_TEMPLATE))
            assertNull(built.getEntry(RegistryZipAssembler.CUSTOMIZE_TEMPLATE))
            assertEquals("actual_file_name=${output.nameWithoutExtension}\nDEBUG=true\n",
                built.getInputStream(built.getEntry("customize.sh")).bufferedReader().readText())
            assertEquals("#MAGISK", built.getInputStream(
                built.getEntry("META-INF/com/google/android/updater-script")).bufferedReader().readText())
            assertEquals("      Created by Nikhil Menghani      ",
                built.getInputStream(built.getEntry("creator.txt")).bufferedReader().readText())
            val installer = built.getInputStream(built.getEntry("common/install.sh")).bufferedReader().readText()
            assertTrue(installer.contains("ProgressBarValues=\""))
            assertTrue(installer.contains("GmsCore,3,product"))
            assertEquals("GmsCore=3\n", built.getInputStream(built.getEntry("common/file_size.txt")).bufferedReader().readText())
        }
    }
    private fun resolved(artifact: Artifact = Artifact("https://example.test/a", "a".repeat(64), 1)) = ResolvedPackage(
        CatalogPackage("gms_core", "gms_core", true, false, emptyList(), mapOf("stable" to "s"), emptyMap()), "s",
        PackageVersion("stable", 1, "test.app", AndroidCompatibility(null, 36, null), listOf("arm64-v8a"), "product",
            ApkMetadata("priv-app/Test/Test.apk", true), artifact), ReleaseChannel.STABLE, false)
}

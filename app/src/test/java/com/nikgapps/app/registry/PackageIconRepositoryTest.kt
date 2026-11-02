package com.nikgapps.app.registry

import org.junit.Assert.*
import org.junit.Test

class PackageIconRepositoryTest {
    private val sha = "a".repeat(64)
    private fun manifest(url: String, size: Int = 123) = """{"schemaVersion":1,"icons":{
        "com.example.app":{"url":"$url","sha256":"$sha","size":$size}}}"""
    private val url get() = "https://gitlab.com/api/v4/projects/85036487/packages/generic/app-icons/$sha/com.example.app.png"
    @Test fun acceptsPublishedAsset() {
        assertEquals(url, PackageIconRepository.parse(manifest(url))["com.example.app"]?.url)
    }
    @Test fun rejectsUntrustedAndOversizedAssets() {
        assertTrue(PackageIconRepository.parse(manifest("https://example.com/icon.png")).isEmpty())
        assertTrue(PackageIconRepository.parse(manifest(url, 600000)).isEmpty())
        assertTrue(PackageIconRepository.parse(manifest(url.replace(sha, "b".repeat(64)))).isEmpty())
    }
    @Test fun rejectsUnsupportedSchema() {
        assertThrows(IllegalArgumentException::class.java) {
            PackageIconRepository.parse(manifest(url).replace("schemaVersion\":1", "schemaVersion\":2"))
        }
    }
}

package com.nikgapps.app.registry

import java.io.File
import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RegistryAvailabilityTest {
    private var previousToken: () -> String = { "" }
    @Before fun reset() {
        previousToken = RegistryDelivery.userToken
        RegistryDelivery.userToken = { "fixture-user-token" }
        RegistryAvailability.clearExpired(Long.MAX_VALUE)
    }
    @After fun cleanup() {
        RegistryDelivery.userToken = previousToken
        RegistryAvailability.clearExpired(Long.MAX_VALUE)
    }

    @Test fun retryAfterSupportsSecondsHttpDatesAndResetFallback() {
        val now = Instant.parse("2026-11-10T03:50:00Z").toEpochMilli()
        assertEquals(now + 600_000, registryRetryAt("600", null, now))
        assertEquals(now + 600_000, registryRetryAt("Tue, 10 Nov 2026 04:00:00 GMT", null, now))
        assertEquals(now + 600_000, registryRetryAt("invalid", "1794283200", now))
        assertEquals(now + 60_000, registryRetryAt(null, null, now))
        assertEquals(now + 60_000, registryRetryAt("-1", null, now))
    }

    @Test fun throttlingReturnsImmediatelyAndStopsFurtherGitlabRequests() {
        var calls = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls++
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(429).message("Too Many Requests").header("Retry-After", "600")
                .body("rate limited".toResponseBody()).build()
        }.build()
        val request = Request.Builder().url(CatalogRepository.CATALOG_URL).build()
        val before = System.currentTimeMillis()
        val first = assertThrows(RegistryRateLimitedException::class.java) {
            client.executeRegistryRequest(request) { it.body.string() }
        }
        assertTrue(first.retryAtMillis >= before + 600_000)
        assertTrue(System.currentTimeMillis() - before < 2_000)
        assertTrue(first.message!!.contains("Retry after"))
        assertThrows(RegistryRateLimitedException::class.java) {
            client.executeRegistryRequest(request) { it.body.string() }
        }
        assertEquals(1, calls)
        // SourceForge and other services must not be blocked by a GitLab cooldown.
        RegistryAvailability.requireAvailable(Request.Builder().url("https://sourceforge.net/file").build().url)
        RegistryAvailability.clearExpired(first.retryAtMillis)
        assertNull(RegistryAvailability.retryAtMillis.value)
    }

    @Test fun cooldownReadsDoNotExtendDeadlineButAnotherServerResponseCan() {
        val now = System.currentTimeMillis()
        val first = RegistryAvailability.rateLimited("600", null, now)
        val url = Request.Builder().url(CatalogRepository.CATALOG_URL).build().url
        repeat(3) {
            assertThrows(RegistryRateLimitedException::class.java) { RegistryAvailability.requireAvailable(url) }
            assertEquals(first, RegistryAvailability.retryAtMillis.value)
        }
        val next = RegistryAvailability.rateLimited("3600", null, now + 1_000)
        assertEquals(now + 3_601_000, next)
    }

    @Test fun staleCatalogFallsBackImmediatelyDuringCooldown() = runBlocking {
        val dir = Files.createTempDirectory("rate-limit-metadata").toFile()
        try {
            val metadata = File(dir, "nikgapps/github-metadata").apply { mkdirs() }
            File(metadata, "catalog.json").writeText(RegistryTestFixtures.catalog())
            File(metadata, "appsets.json").writeText(RegistryTestFixtures.appSets())
            val assets = RegistryZipAssembler.REQUIRED_ASSETS.joinToString(",") {
                "\"$it\":{\"url\":\"https://gitlab.com/asset\",\"sha256\":\"a\",\"size\":1}"
            }
            File(metadata, "builder-assets.json").writeText("""{"schemaVersion":1,"assets":{$assets}}""")
            File(metadata, "config-templates.json").writeText("""{"schemaVersion":1,"templates":{
                "16":{"androidVersion":"16","version":"38","template":{"url":"https://gitlab.com/config","sha256":"a","size":1}}}}""")
            var calls = 0
            val client = OkHttpClient.Builder().addInterceptor { calls++; error("Network must not be used during cooldown") }.build()
            RegistryAvailability.rateLimited("600", null)
            val loaded = CatalogRepository(dir, client).load(forceRefresh = true)
            assertTrue(loaded.fromCache)
            assertEquals(3, loaded.catalog.packages.size)
            assertEquals(0, calls)
        } finally { dir.deleteRecursively() }
    }

    @Test fun verifiedPackageCacheIsUsableButMissingPackagesFailWithRetryTime() = runBlocking {
        val dir = Files.createTempDirectory("rate-limit-packages").toFile()
        try {
            val bytes = "cached package".toByteArray()
            val sha = RegistryTestFixtures.hash(bytes)
            val target = File(dir, "nikgapps/packages/$sha.zip").apply { parentFile!!.mkdirs(); writeBytes(bytes) }
            val catalogPackage = CatalogParser.parseCatalog(RegistryTestFixtures.catalog()).packages.first()
            val version = catalogPackage.versions.getValue("s").copy(
                artifact = Artifact("https://api.github.com/repos/nikgapps/nikgapps-packages/releases/assets/123", sha, bytes.size.toLong()))
            val pkg = ResolvedPackage(catalogPackage, "s", version, ReleaseChannel.STABLE, false)
            var calls = 0
            val client = OkHttpClient.Builder().addInterceptor { calls++; error("Network must not be used during cooldown") }.build()
            RegistryAvailability.rateLimited("600", null)
            assertEquals(target, ArtifactDownloader(dir, client).obtain(pkg))
            target.delete()
            try { ArtifactDownloader(dir, client).obtain(pkg); fail("Missing package should explain the cooldown") }
            catch (error: RegistryRateLimitedException) { assertTrue(error.message!!.contains("uncached packages")) }
            assertEquals(0, calls)
        } finally { dir.deleteRecursively() }
    }
}

package com.nikgapps.app.registry

import okhttp3.Request
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking

class RegistryDeliveryTest {
    @Test fun catalogUsesOnePinnedCommitAndUserTokenThenReusesTheSnapshot() = runBlocking {
        val directory = Files.createTempDirectory("github-metadata").toFile()
        val previousToken = RegistryDelivery.userToken
        RegistryDelivery.userToken = { "fixture-user-token" }
        RegistryAvailability.clearExpired(Long.MAX_VALUE)
        try {
            val commit = "a".repeat(40)
            val assets = RegistryZipAssembler.REQUIRED_ASSETS.joinToString(",") {
                "\"$it\":{\"url\":\"${RegistryDelivery.API}/releases/assets/123\",\"sha256\":\"a\",\"size\":1}"
            }
            val documents = mapOf(
                "catalog.json" to RegistryTestFixtures.catalog(),
                "appsets.json" to RegistryTestFixtures.appSets(),
                "builder-assets.json" to """{"schemaVersion":1,"assets":{$assets}}""",
                "config-templates.json" to """{"schemaVersion":1,"templates":{"16":{"androidVersion":"16","version":"38",
                    "template":{"url":"${RegistryDelivery.API}/releases/assets/123","sha256":"a","size":1}}}}""",
                "releases/index.json" to """{"schemaVersion":1,"updatedAt":"2026-11-10T00:00:00Z","latest":{},"releases":[]}"""
            )
            var calls = 0
            val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
                val request = chain.request(); calls++
                assertEquals("Bearer fixture-user-token", request.header("Authorization"))
                val text = if (request.url.encodedPath.endsWith("/git/ref/heads/main")) {
                    """{"object":{"sha":"$commit"}}"""
                } else {
                    assertEquals(commit, request.url.queryParameter("ref"))
                    assertEquals("application/vnd.github.raw+json", request.header("Accept"))
                    documents.getValue(request.url.encodedPath.substringAfter("/contents/"))
                }
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                    .body(text.toResponseBody()).build()
            }.build()
            val repository = CatalogRepository(directory, client)
            assertFalse(repository.load().fromCache)
            assertEquals(6, calls)
            assertTrue(repository.load().fromCache)
            assertEquals(6, calls)
        } finally {
            RegistryDelivery.userToken = previousToken
            RegistryAvailability.clearExpired(Long.MAX_VALUE)
            directory.deleteRecursively()
        }
    }

    @Test fun authenticatesOnlyTrustedGithubRepositoryAndSelectsCorrectMediaType() {
        val metadata = RegistryDelivery.prepare(Request.Builder().url(RegistryDelivery.contents("catalog.json")).build(), "user-token")
        assertEquals("Bearer user-token", metadata.header("Authorization"))
        assertEquals("application/vnd.github.raw+json", metadata.header("Accept"))
        val asset = RegistryDelivery.prepare(Request.Builder().url("${RegistryDelivery.API}/releases/assets/123")
            .header("Range", "bytes=100-").build(), "user-token")
        assertEquals("application/octet-stream", asset.header("Accept"))
        assertEquals("bytes=100-", asset.header("Range"))
        assertEquals("Bearer user-token", asset.header("Authorization"))
        for (url in listOf("https://sourceforge.net/package", "https://release-assets.githubusercontent.com/file",
            "https://api.github.com/repos/attacker/repo/releases/assets/123", "https://api.github.com.attacker.test/file",
            "https://api.github.com:8443/repos/nikgapps/nikgapps-packages/releases/assets/123")) {
            val request = RegistryDelivery.prepare(Request.Builder().url(url).header("Authorization", "Bearer secret").build(), "user-token")
            assertNull(request.header("Authorization"))
        }
        assertThrows(MetadataException::class.java) {
            RegistryDelivery.prepare(Request.Builder().url("https://gitlab.com/package.zip").build(), "user-token")
        }
        assertThrows(com.nikgapps.app.utils.network.GitHubBuildAuth.AuthException::class.java) {
            RegistryDelivery.prepare(Request.Builder().url(RegistryDelivery.contents("catalog.json")).build(), "")
        }
    }

    @Test fun githubPrimaryAndSecondaryLimitsAreRecognizedWithoutMistakingPermissionsForLimits() {
        val request = Request.Builder().url("${RegistryDelivery.API}/releases/assets/123").build()
        fun response(code: Int, header: Pair<String, String>? = null): Response = Response.Builder()
            .request(request).protocol(Protocol.HTTP_1_1).code(code).message("fixture").body("".toResponseBody())
            .apply { header?.let { header(it.first, it.second) } }.build()
        assertTrue(RegistryDelivery.rateLimited(response(429)))
        assertTrue(RegistryDelivery.rateLimited(response(403, "X-RateLimit-Remaining" to "0")))
        assertTrue(RegistryDelivery.rateLimited(response(403, "Retry-After" to "60")))
        assertFalse(RegistryDelivery.rateLimited(response(403)))
        assertFalse(RegistryDelivery.rateLimited(response(401)))
    }
}

package com.nikgapps.app.registry

import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import com.nikgapps.app.utils.AppDiagnostics

data class BuilderAsset(val url: String, val sha256: String, val size: Long)
data class RegistryMetadata(val catalog: Catalog, val appSets: AppSetCatalog,
    val builderAssets: Map<String, BuilderAsset>, val releaseIndex: ReleaseIndex?,
    val release: CatalogRelease?, val fromCache: Boolean)

fun catalogAndroidVersion(displayName: String): String =
    displayName.removePrefix("Android ").replace("12L", "12.1")

class CatalogRepository(private val cacheDirectory: File, private val client: OkHttpClient = OkHttpClient()) {
    suspend fun load(androidVersion: String? = null, channel: String = "stable",
        architecture: String = "arm64-v8a", releaseId: String? = null): RegistryMetadata = withContext(Dispatchers.IO) {
        val directory = File(cacheDirectory, "nikgapps/metadata").apply { mkdirs() }
        val catalogCache = File(directory, "catalog.json")
        val appSetsCache = File(directory, "appsets.json")
        val builderAssetsCache = File(directory, "builder-assets.json")
        val releaseIndexCache = File(directory, "release-index.json")
        try {
            val catalogText = download(CATALOG_URL)
            val appSetsText = download(APPSETS_URL)
            val builderAssetsText = download(BUILDER_ASSETS_URL)
            val releaseIndexText = runCatching { download(RELEASE_INDEX_URL) }.getOrNull()
            val index = releaseIndexText?.let(CatalogParser::parseReleaseIndex)
            val summary = selectRelease(index, androidVersion, channel, architecture, releaseId)
            val releaseText = summary?.let { download("$METADATA_BASE_URL/${it.manifest}") }
            val parsed = parsePair(catalogText, appSetsText, builderAssetsText,
                releaseIndexText, releaseText, false)
            atomicWrite(catalogCache, catalogText)
            atomicWrite(appSetsCache, appSetsText)
            atomicWrite(builderAssetsCache, builderAssetsText)
            releaseIndexText?.let { atomicWrite(releaseIndexCache, it) }
            if (summary != null && releaseText != null) atomicWrite(File(directory, "release-${summary.id}.json"), releaseText)
            AppDiagnostics.info("metadata", "loaded", mapOf("source" to "network",
                "android" to (parsed.release?.androidVersion ?: parsed.catalog.androidVersion),
                "release" to parsed.release?.id, "packages" to parsed.catalog.packages.size))
            parsed
        } catch (networkOrMetadata: Exception) {
            if (!catalogCache.isFile || !appSetsCache.isFile || !builderAssetsCache.isFile) throw MetadataException(
                "Unable to load valid NikGapps metadata and no offline cache is available: ${networkOrMetadata.message}", networkOrMetadata)
            try {
                val indexText = releaseIndexCache.takeIf(File::isFile)?.readText()
                val index = indexText?.let(CatalogParser::parseReleaseIndex)
                val summary = selectRelease(index, androidVersion, channel, architecture, releaseId)
                val releaseText = summary?.let { File(directory, "release-${it.id}.json").takeIf(File::isFile)?.readText() }
                parsePair(catalogCache.readText(), appSetsCache.readText(), builderAssetsCache.readText(),
                    indexText, releaseText, true).also {
                AppDiagnostics.info("metadata", "loaded", mapOf("source" to "cache",
                    "android" to (it.release?.androidVersion ?: it.catalog.androidVersion),
                    "release" to it.release?.id, "packages" to it.catalog.packages.size,
                    "networkError" to networkOrMetadata.javaClass.simpleName))
                }
            }
            catch (cacheError: Exception) { throw MetadataException("Downloaded metadata failed and cached metadata is invalid: ${cacheError.message}", cacheError) }
        }
    }
    private fun selectRelease(index: ReleaseIndex?, androidVersion: String?, channel: String,
        architecture: String, releaseId: String?): ReleaseSummary? {
        if (androidVersion == null) return null
        requireNotNull(index) { "Release metadata is unavailable for Android $androidVersion" }
        val selectedId = releaseId ?: index.latest[androidVersion]?.get(channel)?.get(architecture)
            ?: throw MetadataException("No $channel release for Android $androidVersion ($architecture)")
        return index.releases.firstOrNull { it.id == selectedId && it.androidVersion == androidVersion &&
            it.channel == channel && it.architecture == architecture }
            ?: throw MetadataException("Release '$selectedId' is absent from release history")
    }
    private fun parsePair(c: String, a: String, b: String, indexText: String?, releaseText: String?,
        cached: Boolean): RegistryMetadata {
        val catalog = CatalogParser.parseCatalog(c)
        val releaseIndex = indexText?.let(CatalogParser::parseReleaseIndex)
        val release = releaseText?.let(CatalogParser::parseRelease)
        val appSets = release?.let { AppSetCatalog(it.schemaVersion, it.appSets) } ?: CatalogParser.parseAppSets(a)
        require(catalog.schemaVersion == appSets.schemaVersion) { "Catalog and AppSet schema versions differ" }
        val ids = catalog.packages.map { it.id }.toSet()
        appSets.appSets.forEach { set -> require(set.packages.all { it in ids }) { "AppSet '${set.id}' references a missing package" } }
        val root = Json.parseToJsonElement(b).jsonObject
        require(root.getValue("schemaVersion").jsonPrimitive.int == SUPPORTED_CATALOG_SCHEMA)
        val assets = root.getValue("assets").jsonObject.mapValues { (_, value) ->
            val asset = value.jsonObject
            BuilderAsset(asset.getValue("url").jsonPrimitive.content,
                asset.getValue("sha256").jsonPrimitive.content.lowercase(),
                asset.getValue("size").jsonPrimitive.long)
        }
        val missingAssets = RegistryZipAssembler.REQUIRED_ASSETS - assets.keys
        require(missingAssets.isEmpty()) {
            "builder-assets.json is missing: ${missingAssets.sorted().joinToString()}"
        }
        release?.packages?.forEach { (id, version) ->
            val pkg = catalog.packages.firstOrNull { it.id == id }
                ?: throw MetadataException("Release references missing package '$id'")
            require(version in pkg.versions) { "Release references missing version '$id:$version'" }
        }
        return RegistryMetadata(catalog, appSets, assets, releaseIndex, release, cached)
    }
    private fun download(url: String): String {
        return client.executeRegistryRequest(Request.Builder().url(url).build()) { response ->
            response.body.string().takeIf { it.isNotBlank() } ?: error("Empty response from $url")
        }
    }
    private fun atomicWrite(target: File, text: String) {
        val part = File(target.parentFile, "${target.name}.part")
        part.writeText(text)
        if (!part.renameTo(target)) { target.delete(); check(part.renameTo(target)) { "Cannot update ${target.name}" } }
    }
    companion object {
        const val CATALOG_URL = "https://gitlab.com/nikgapps/nikgapps-package-catalog/-/raw/main/catalog.json"
        const val APPSETS_URL = "https://gitlab.com/nikgapps/nikgapps-package-catalog/-/raw/main/appsets.json"
        const val BUILDER_ASSETS_URL = "https://gitlab.com/nikgapps/nikgapps-package-catalog/-/raw/main/builder-assets.json"
        const val METADATA_BASE_URL = "https://gitlab.com/nikgapps/nikgapps-package-catalog/-/raw/main"
        const val RELEASE_INDEX_URL = "$METADATA_BASE_URL/releases/index.json"
        const val GITLAB_PROJECT_ID = 85036487
    }
}

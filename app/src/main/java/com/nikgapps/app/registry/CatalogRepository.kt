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
    val builderAssets: Map<String, BuilderAsset>, val fromCache: Boolean)

class CatalogRepository(private val cacheDirectory: File, private val client: OkHttpClient = OkHttpClient()) {
    suspend fun load(): RegistryMetadata = withContext(Dispatchers.IO) {
        val directory = File(cacheDirectory, "nikgapps/metadata").apply { mkdirs() }
        val catalogCache = File(directory, "catalog.json")
        val appSetsCache = File(directory, "appsets.json")
        val builderAssetsCache = File(directory, "builder-assets.json")
        try {
            val catalogText = download(CATALOG_URL)
            val appSetsText = download(APPSETS_URL)
            val builderAssetsText = download(BUILDER_ASSETS_URL)
            val parsed = parsePair(catalogText, appSetsText, builderAssetsText, false)
            atomicWrite(catalogCache, catalogText)
            atomicWrite(appSetsCache, appSetsText)
            atomicWrite(builderAssetsCache, builderAssetsText)
            AppDiagnostics.info("metadata", "loaded", mapOf("source" to "network",
                "android" to parsed.catalog.androidVersion, "packages" to parsed.catalog.packages.size))
            parsed
        } catch (networkOrMetadata: Exception) {
            if (!catalogCache.isFile || !appSetsCache.isFile || !builderAssetsCache.isFile) throw MetadataException(
                "Unable to load valid NikGapps metadata and no offline cache is available: ${networkOrMetadata.message}", networkOrMetadata)
            try { parsePair(catalogCache.readText(), appSetsCache.readText(), builderAssetsCache.readText(), true).also {
                AppDiagnostics.info("metadata", "loaded", mapOf("source" to "cache",
                    "android" to it.catalog.androidVersion, "packages" to it.catalog.packages.size,
                    "networkError" to networkOrMetadata.javaClass.simpleName))
            } }
            catch (cacheError: Exception) { throw MetadataException("Downloaded metadata failed and cached metadata is invalid: ${cacheError.message}", cacheError) }
        }
    }
    private fun parsePair(c: String, a: String, b: String, cached: Boolean): RegistryMetadata {
        val catalog = CatalogParser.parseCatalog(c)
        val appSets = CatalogParser.parseAppSets(a)
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
        return RegistryMetadata(catalog, appSets, assets, cached)
    }
    private fun download(url: String): String {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code} for $url")
            return response.body.string().takeIf { it.isNotBlank() } ?: error("Empty response from $url")
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
        const val GITLAB_PROJECT_ID = 85036487
    }
}

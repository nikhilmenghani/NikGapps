package com.nikgapps.app.registry

import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RegistryMetadata(val catalog: Catalog, val appSets: AppSetCatalog, val fromCache: Boolean)

class CatalogRepository(private val cacheDirectory: File, private val client: OkHttpClient = OkHttpClient()) {
    suspend fun load(): RegistryMetadata = withContext(Dispatchers.IO) {
        val directory = File(cacheDirectory, "nikgapps/metadata").apply { mkdirs() }
        val catalogCache = File(directory, "catalog.json")
        val appSetsCache = File(directory, "appsets.json")
        try {
            val catalogText = download(CATALOG_URL)
            val appSetsText = download(APPSETS_URL)
            val parsed = parsePair(catalogText, appSetsText, false)
            atomicWrite(catalogCache, catalogText)
            atomicWrite(appSetsCache, appSetsText)
            parsed
        } catch (networkOrMetadata: Exception) {
            if (!catalogCache.isFile || !appSetsCache.isFile) throw MetadataException(
                "Unable to load valid NikGapps metadata and no offline cache is available: ${networkOrMetadata.message}", networkOrMetadata)
            try { parsePair(catalogCache.readText(), appSetsCache.readText(), true) }
            catch (cacheError: Exception) { throw MetadataException("Downloaded metadata failed and cached metadata is invalid: ${cacheError.message}", cacheError) }
        }
    }
    private fun parsePair(c: String, a: String, cached: Boolean): RegistryMetadata {
        val catalog = CatalogParser.parseCatalog(c)
        val appSets = CatalogParser.parseAppSets(a)
        require(catalog.schemaVersion == appSets.schemaVersion) { "Catalog and AppSet schema versions differ" }
        val ids = catalog.packages.map { it.id }.toSet()
        appSets.appSets.forEach { set -> require(set.packages.all { it in ids }) { "AppSet '${set.id}' references a missing package" } }
        return RegistryMetadata(catalog, appSets, cached)
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
        const val GITLAB_PROJECT_ID = 85036487
    }
}

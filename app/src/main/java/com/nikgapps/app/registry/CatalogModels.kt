package com.nikgapps.app.registry

import kotlinx.serialization.json.*

const val SUPPORTED_CATALOG_SCHEMA = 1

enum class ReleaseChannel { STABLE, BETA, CANARY;
    val wireName get() = name.lowercase()
}

data class Catalog(val schemaVersion: Int, val updatedAt: String, val androidVersion: String,
    val platformApi: Int, val architecture: String, val packages: List<CatalogPackage>)
data class CatalogPackage(val id: String, val name: String, val selectable: Boolean,
    val internal: Boolean, val dependencies: List<Dependency>, val channels: Map<String, String>,
    val versions: Map<String, PackageVersion>)
data class Dependency(val id: String, val whenAppSet: String? = null)
data class PackageVersion(val versionName: String, val versionCode: Long, val packageName: String?,
    val android: AndroidCompatibility, val architectures: List<String>, val defaultPartition: String,
    val apk: ApkMetadata?, val artifact: Artifact, val files: List<CatalogFile> = emptyList(),
    val install: InstallMetadata? = null)
data class CatalogFile(val path: String, val archivePath: String?, val installPath: String?,
    val sha256: String, val size: Long, val type: String)
data class InstallMetadata(val format: String, val title: String, val packageTitle: String,
    val payloadSize: Long, val removeFiles: List<String>, val removeOverlays: List<String>,
    val privilegedPermissions: List<String>, val cleanFlashOnly: Boolean,
    val addonIndex: String)
data class AndroidCompatibility(val minApi: Int?, val targetApi: Int?, val maxApi: Int?)
data class ApkMetadata(val path: String, val replaceable: Boolean)
data class Artifact(val url: String, val sha256: String, val size: Long?)
data class AppSetCatalog(val schemaVersion: Int, val appSets: List<CatalogAppSet>)
data class CatalogAppSet(val id: String, val name: String, val packages: List<String>,
    val resolvedPackages: List<String>, val legacyPackageNames: Map<String, String>)

class MetadataException(message: String, cause: Throwable? = null) : Exception(message, cause)

object CatalogParser {
    private val json = Json { ignoreUnknownKeys = false; isLenient = false }
    fun parseCatalog(text: String): Catalog = wrap("catalog") {
        val root = json.parseToJsonElement(text).jsonObject
        schema(root)
        Catalog(root.int("schemaVersion"), root.string("updatedAt"), root.string("androidVersion"),
            root.int("platformApi"), root.string("architecture"), root.array("packages").map { item ->
                val p = item.jsonObject
                CatalogPackage(p.string("id"), p.string("name"), p.bool("selectable"),
                    p.bool("internal"), p.array("dependencies").map { dependency ->
                        val d = dependency.jsonObject
                        Dependency(d.string("id"), d["when"]?.jsonObject?.get("appSet")?.jsonPrimitive?.content)
                    }, p.obj("channels").mapValues { it.value.jsonPrimitive.content },
                    p.obj("versions").mapValues { (_, value) -> version(value.jsonObject) })
            }).also { catalog ->
                require(catalog.packages.isNotEmpty()) { "packages is empty" }
                require(catalog.packages.map { it.id }.distinct().size == catalog.packages.size) { "duplicate package id" }
            }
    }

    fun parseAppSets(text: String): AppSetCatalog = wrap("AppSet metadata") {
        val root = json.parseToJsonElement(text).jsonObject
        schema(root)
        AppSetCatalog(root.int("schemaVersion"), root.array("appSets").map { item ->
            val a = item.jsonObject
            CatalogAppSet(a.string("id"), a.string("name"), a.strings("packages"),
                a.strings("resolvedPackages"), a.obj("legacyPackageNames").mapValues { it.value.jsonPrimitive.content })
        }).also { require(it.appSets.isNotEmpty()) { "appSets is empty" } }
    }

    private fun version(v: JsonObject): PackageVersion {
        val android = v.obj("android")
        val artifact = v.obj("artifact")
        return PackageVersion(v.string("versionName"), v.long("versionCode"),
            v["packageName"].nullableString(), AndroidCompatibility(android.nullableInt("minApi"),
                android.nullableInt("targetApi"), android.nullableInt("maxApi")), v.strings("architectures"),
            v.string("defaultPartition"), v["apk"]?.takeUnless { it is JsonNull }?.jsonObject?.let {
                ApkMetadata(it.string("path"), it.bool("replaceable"))
            }, Artifact(artifact.string("url"), artifact.string("sha256").lowercase(), artifact.nullableLong("size")),
            v["files"]?.jsonArray?.map { value -> val f = value.jsonObject
                CatalogFile(f.string("path"), f.nullableString("archivePath"), f.nullableString("installPath"),
                    f.string("sha256").lowercase(), f.long("size"), f["type"]?.jsonPrimitive?.content ?: "supportingFile")
            }.orEmpty(), v["install"]?.takeUnless { it is JsonNull }?.jsonObject?.let { install ->
                InstallMetadata(install.string("format"), install.string("title"), install.string("packageTitle"),
                    install.long("payloadSize"), install.optionalStrings("removeFiles"),
                    install.optionalStrings("removeOverlays"), install.optionalStrings("privilegedPermissions"),
                    install["cleanFlashOnly"]?.jsonPrimitive?.boolean ?: false,
                    install["addonIndex"]?.jsonPrimitive?.content ?: "09")
            })
            .also { require(it.artifact.sha256.matches(Regex("[0-9a-f]{64}"))) { "invalid artifact sha256" } }
    }

    private fun schema(root: JsonObject) = require(root.int("schemaVersion") == SUPPORTED_CATALOG_SCHEMA) {
        "Unsupported schemaVersion ${root.int("schemaVersion")}; expected $SUPPORTED_CATALOG_SCHEMA"
    }
    private inline fun <T> wrap(name: String, block: () -> T): T = try { block() } catch (e: Exception) {
        if (e is MetadataException) throw e else throw MetadataException("Invalid $name: ${e.message}", e)
    }
    private fun JsonObject.string(k: String) = getValue(k).jsonPrimitive.content.also { require(it.isNotBlank()) { "$k is blank" } }
    private fun JsonObject.int(k: String) = getValue(k).jsonPrimitive.int
    private fun JsonObject.long(k: String) = getValue(k).jsonPrimitive.long
    private fun JsonObject.bool(k: String) = getValue(k).jsonPrimitive.boolean
    private fun JsonObject.array(k: String) = getValue(k).jsonArray
    private fun JsonObject.obj(k: String) = getValue(k).jsonObject
    private fun JsonObject.strings(k: String) = array(k).map { it.jsonPrimitive.content }
    private fun JsonObject.optionalStrings(k: String) = get(k)?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
    private fun JsonObject.nullableString(k: String) = get(k).let { if (it == null || it is JsonNull) null else it.jsonPrimitive.content }
    private fun JsonElement?.nullableString() = if (this == null || this is JsonNull) null else jsonPrimitive.content
    private fun JsonObject.nullableInt(k: String) = get(k).let { if (it == null || it is JsonNull) null else it.jsonPrimitive.int }
    private fun JsonObject.nullableLong(k: String) = get(k).let { if (it == null || it is JsonNull) null else it.jsonPrimitive.long }
}

private val NEVER_SELECTABLE = setOf("extrafiles", "extrafilesgo", "gms_core_support_common",
    "gms_core_support_standard", "gms_core_support_go")
fun Catalog.publicPackages(appSet: CatalogAppSet): List<CatalogPackage> {
    val byId = packages.associateBy { it.id }
    return appSet.packages.mapNotNull(byId::get).filter { it.selectable && !it.internal &&
        it.id.lowercase() !in NEVER_SELECTABLE && it.name.lowercase() !in NEVER_SELECTABLE }
}

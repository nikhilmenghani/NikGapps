package com.nikgapps.app.registry

import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlinx.serialization.json.*

data class PayloadFile(val path: String, val sha256: String, val size: Long)
data class PackageDescriptor(val schemaVersion: Int, val id: String, val packageName: String?,
    val defaultPartition: String, val apk: ApkMetadata?, val files: List<PayloadFile>,
    val removeFiles: List<String> = emptyList(), val privilegedPermissions: List<String> = emptyList())
class InvalidPackageZip(message: String) : Exception(message)

class PackageZipValidator {
    fun validate(file: File, expected: ResolvedPackage): PackageDescriptor = try {
        ZipFile(file).use { zip ->
            val names = mutableSetOf<String>()
            val entries = zip.entries().asSequence().toList()
            entries.forEach { entry ->
                val normalized = entry.name.replace('\\', '/')
                if (normalized.startsWith('/') || Regex("^[A-Za-z]:").containsMatchIn(normalized) ||
                    normalized.split('/').any { it == ".." }) throw InvalidPackageZip("Unsafe ZIP path '${entry.name}'")
                if (!names.add(normalized)) throw InvalidPackageZip("Duplicate ZIP entry '$normalized'")
            }
            val metadataEntry = zip.getEntry("package.json") ?: throw InvalidPackageZip("Missing package.json")
            val root = Json.parseToJsonElement(zip.getInputStream(metadataEntry).bufferedReader().readText()).jsonObject
            val descriptor = PackageDescriptor(root.reqInt("schemaVersion"), root.reqString("id"), root.nullString("packageName"),
                root.reqString("defaultPartition"), root["apk"]?.takeUnless { it is JsonNull }?.jsonObject?.let {
                    ApkMetadata(it.reqString("path"), it["replaceable"]?.jsonPrimitive?.boolean ?: false)
                }, root.getValue("files").jsonArray.map { value -> val f = value.jsonObject
                    PayloadFile(f.reqString("path"), f.reqString("sha256").lowercase(), f.getValue("size").jsonPrimitive.long)
                }, root["removeFiles"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
                root["privilegedPermissions"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty())
            if (descriptor.schemaVersion != SUPPORTED_CATALOG_SCHEMA) throw InvalidPackageZip("Unsupported package schemaVersion ${descriptor.schemaVersion}")
            if (descriptor.id != expected.catalogPackage.id) throw InvalidPackageZip("package.json id '${descriptor.id}' does not match '${expected.catalogPackage.id}'")
            val listed = descriptor.files.map { it.path }.toSet()
            if (listed.size != descriptor.files.size) throw InvalidPackageZip("Duplicate payload metadata path")
            descriptor.files.forEach { payload ->
                safeRelative(payload.path)
                val entry = zip.getEntry("payload/default/${payload.path}")
                    ?: throw InvalidPackageZip("Missing payload file '${payload.path}'")
                if (entry.size != payload.size) throw InvalidPackageZip("Payload size mismatch for '${payload.path}'")
                val digest = MessageDigest.getInstance("SHA-256")
                zip.getInputStream(entry).use { input -> val buffer = ByteArray(64 * 1024)
                    while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) } }
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (actual != payload.sha256) throw InvalidPackageZip("Payload checksum mismatch for '${payload.path}'")
            }
            descriptor.apk?.let { apk ->
                safeRelative(apk.path)
                if (zip.getEntry("payload/default/${apk.path}") == null) throw InvalidPackageZip("Missing primary APK '${apk.path}'")
            }
            entries.filter { !it.isDirectory && it.name.startsWith("payload/default/") }.forEach {
                val relative = it.name.removePrefix("payload/default/")
                if (relative !in listed) throw InvalidPackageZip("Unlisted payload file '$relative'")
            }
            descriptor
        }
    } catch (e: InvalidPackageZip) { throw e } catch (e: Exception) { throw InvalidPackageZip("Invalid ZIP for '${expected.catalogPackage.id}': ${e.message}") }

    fun installPath(path: String, defaultPartition: String): String {
        safeRelative(path)
        val first = path.substringBefore('/')
        return if (first in EXPLICIT_PARTITIONS) path else "$defaultPartition/$path"
    }
    private fun safeRelative(path: String) {
        val p = path.replace('\\', '/')
        if (p.startsWith('/') || Regex("^[A-Za-z]:").containsMatchIn(p) || p.split('/').any { it == ".." })
            throw InvalidPackageZip("Unsafe payload path '$path'")
    }
    companion object { val EXPLICIT_PARTITIONS = setOf("product", "system", "system_ext", "vendor") }
}
private fun JsonObject.reqString(k: String) = getValue(k).jsonPrimitive.content
private fun JsonObject.reqInt(k: String) = getValue(k).jsonPrimitive.int
private fun JsonObject.nullString(k: String) = get(k).let { if (it == null || it is JsonNull) null else it.jsonPrimitive.content }

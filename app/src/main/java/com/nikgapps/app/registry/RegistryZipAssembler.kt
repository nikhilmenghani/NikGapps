package com.nikgapps.app.registry

import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.*
import kotlin.math.round

data class BuildRequest(val androidVersion: String, val api: Int, val architecture: String,
    val appSet: CatalogAppSet, val defaultChannel: ReleaseChannel, val channelOverrides: Map<String, ReleaseChannel>,
    val selectedIds: Set<String>, val timestamp: Instant = Instant.now(),
    val packageAppSets: Map<String, CatalogAppSet> = emptyMap())
data class ValidatedArtifact(val resolved: ResolvedPackage, val file: File, val descriptor: PackageDescriptor)

/** Shared files are the unmodified Python-builder assets keyed by their final ZIP path. */
fun interface BuilderAssetSource { fun assets(): Map<String, ByteArray> }

class RegistryZipAssembler(private val assetSource: BuilderAssetSource) {
    fun build(outputDirectory: File, request: BuildRequest, artifacts: List<ValidatedArtifact>): File {
        require(artifacts.isNotEmpty()) { "No resolved packages" }
        val expected = artifacts.map { it.resolved.catalogPackage.id }
        require(expected.distinct().size == expected.size) { "Duplicate resolved package" }
        val date = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC).format(request.timestamp)
        val appSetIds = request.packageAppSets.values.map { it.id }.distinct().ifEmpty { listOf(request.appSet.id) }
        val variant = if (appSetIds.size > 1) "custom" else appSetIds.single()
        val output = File(outputDirectory.apply { mkdirs() }, "NikGapps-$variant-${request.architecture}-$date.zip")
        val part = File(output.parentFile, "${output.name}.part")
        try {
            ZipOutputStream(part.outputStream().buffered()).use { finalZip ->
                val assets = assetSource.assets()
                REQUIRED_ASSETS.forEach { require(assets.containsKey(it)) { "Missing builder asset '$it'" } }
                assets.filterKeys { !it.startsWith("@template/") }.toSortedMap()
                    .forEach { (path, bytes) -> finalZip.bytes(path, bytes) }
                val packageSizes = StringBuilder()
                val packageRows = linkedMapOf<String, MutableList<Triple<String, Long, String>>>()
                artifacts.forEach { artifact ->
                    val packageAppSet = request.packageAppSets[artifact.resolved.catalogPackage.id] ?: request.appSet
                    val title = packageAppSet.legacyPackageNames[artifact.resolved.catalogPackage.id]
                        ?: artifact.resolved.catalogPackage.name.replace(Regex("[^A-Za-z0-9_-]"), "")
                    require(artifact.descriptor.install != null) {
                        "Package '${artifact.resolved.catalogPackage.id}' is not a prebuilt NikGapps package"
                    }
                    val payloadSize = artifact.descriptor.install.payloadSize
                    finalZip.file("AppSet/${packageAppSet.name}/$title.zip", artifact.file)
                    packageSizes.append(title).append('=').append(payloadSize).append('\n')
                    packageRows.getOrPut(packageAppSet.name) { mutableListOf() } +=
                        Triple(title, payloadSize, artifact.descriptor.defaultPartition)
                }
                finalZip.text("common/file_size.txt", packageSizes.toString())
                finalZip.text("common/install.sh", finalInstaller(packageRows))
                finalZip.text("afzc/nikgapps.config", config(
                    request, artifacts, assets.getValue(CONFIG_TEMPLATE).decodeToString()))
                finalZip.text("customize.sh", "actual_file_name=${output.nameWithoutExtension}\n" +
                    assets.getValue(CUSTOMIZE_TEMPLATE).decodeToString())
                finalZip.text("nikgapps/build-manifest.json", manifest(request, artifacts))
                finalZip.text("zip_name.txt", output.nameWithoutExtension)
                finalZip.text("creator.txt", "Created by Nikhil Menghani".padStart(32).padEnd(38))
                finalZip.text("META-INF/com/google/android/updater-script", "#MAGISK")
            }
            if (!part.renameTo(output)) error("Cannot publish ${output.name}")
            return output
        } catch (e: Exception) { part.delete(); output.delete(); throw e }
        finally { }
    }
    private fun finalInstaller(groups: Map<String, List<Triple<String, Long, String>>>) = buildString {
        append("#!/sbin/sh\n# Shell Script EDIFY Replacement\n\n")
        val ordered = groups.entries.sortedWith(compareBy<Map.Entry<String, List<Triple<String, Long, String>>>> {
            when (it.key) { "Core", "CoreGo" -> 0; "SetupWizard", "PixelSetupWizard" -> 1; else -> 2 }
        }.thenByDescending { if (it.key in setOf("Core", "CoreGo", "SetupWizard", "PixelSetupWizard")) 0L else it.value.sumOf { row -> row.second } })
        val totalPackages = ordered.sumOf { it.value.size }
        val progressPerPackage = if (totalPackages == 0) 0.0 else round((0.9 / totalPackages) * 100) / 100
        var progress = 0.0
        append("ProgressBarValues=\"\n")
        ordered.forEach { (_, rows) -> rows.sortedByDescending { it.second }.forEach { row ->
            progress = (progress + progressPerPackage).coerceAtMost(1.0)
            append(row.first).append('=').append(round(progress * 100) / 100).append('\n')
        } }
        append("\"\n\n")
        ordered.forEach { (appSet, rows) ->
            append("$appSet=\"\n")
            rows.sortedByDescending { it.second }.forEach { append("${it.first},${it.second},${it.third}\n") }
            append("\"\n\n")
        }
        ordered.forEach { (appSet) -> append("install_app_set \"$appSet\" \"$$appSet\" \".zip\" \n") }
        append("\nset_progress 1.00\n\nexit_install\n\n")
    }
    private fun config(r: BuildRequest, artifacts: List<ValidatedArtifact>, template: String) =
        template.replace(Regex("(?m)^AndroidVersion=.*$"), "AndroidVersion=${r.androidVersion.filter { it.isDigit() }}")
    private fun manifest(r: BuildRequest, artifacts: List<ValidatedArtifact>) = buildJsonObject {
        put("catalogSchemaVersion", SUPPORTED_CATALOG_SCHEMA); put("buildTimestamp", r.timestamp.toString())
        put("androidVersion", r.androidVersion); put("androidApi", r.api); put("architecture", r.architecture)
        put("selectedAppSet", r.appSet.id)
        put("selectedAppSets", buildJsonArray {
            r.packageAppSets.values.map { it.id }.distinct().ifEmpty { listOf(r.appSet.id) }.sorted().forEach { add(it) }
        })
        put("defaultChannel", r.defaultChannel.wireName)
        put("channelOverrides", buildJsonObject { r.channelOverrides.toSortedMap(compareBy { it }).forEach { (id, c) -> put(id, c.wireName) } })
        put("packages", buildJsonArray { artifacts.forEach { a -> add(buildJsonObject {
            put("id", a.resolved.catalogPackage.id); put("hidden", a.resolved.hidden); put("channel", a.resolved.channel.wireName)
            put("versionKey", a.resolved.versionKey); put("versionName", a.resolved.version.versionName)
            put("artifactSha256", a.resolved.version.artifact.sha256)
        }) } })
    }.toString()
    companion object {
        const val CONFIG_TEMPLATE = "@template/nikgapps.config"
        const val CUSTOMIZE_TEMPLATE = "@template/customize.sh"
        val REQUIRED_ASSETS = setOf(
            "META-INF/com/google/android/update-binary", "afzc/debloater.config", "changelog.yaml",
            "common/functions.sh", "common/nikgapps_functions.sh", "common/addon.sh", "common/header.sh",
            "common/mount.sh", "common/mtg_mount.sh", "common/unmount.sh", "module.prop",
            "busybox", CONFIG_TEMPLATE, CUSTOMIZE_TEMPLATE)
    }
}
private fun ZipOutputStream.text(path: String, value: String) = bytes(path, value.toByteArray())
private fun ZipOutputStream.bytes(path: String, value: ByteArray) { putNextEntry(ZipEntry(path).apply { time = 0 }); write(value); closeEntry() }
private fun ZipOutputStream.stream(path: String, input: java.io.InputStream) { putNextEntry(ZipEntry(path).apply { time = 0 }); input.copyTo(this); closeEntry() }
private fun ZipOutputStream.file(path: String, file: File) {
    val crc = java.util.zip.CRC32()
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(128 * 1024)
        while (true) { val count = input.read(buffer); if (count < 0) break; crc.update(buffer, 0, count) }
    }
    putNextEntry(ZipEntry(path).apply {
        time = 0; method = ZipEntry.STORED; size = file.length(); compressedSize = file.length(); this.crc = crc.value
    })
    file.inputStream().buffered().use { it.copyTo(this) }
    closeEntry()
}

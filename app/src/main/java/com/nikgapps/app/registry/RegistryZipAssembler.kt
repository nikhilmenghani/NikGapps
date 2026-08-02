package com.nikgapps.app.registry

import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.*

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
        val nestedDirectory = File(outputDirectory, ".${output.name}.packages").apply { mkdirs() }
        try {
            ZipOutputStream(part.outputStream().buffered()).use { finalZip ->
                val assets = assetSource.assets()
                REQUIRED_ASSETS.forEach { require(assets.containsKey(it)) { "Missing builder asset '$it'" } }
                assets.toSortedMap().forEach { (path, bytes) -> finalZip.bytes(path, bytes) }
                val packageSizes = StringBuilder()
                val packageRows = linkedMapOf<String, MutableList<Triple<String, Long, String>>>()
                artifacts.forEach { artifact ->
                    val packageAppSet = request.packageAppSets[artifact.resolved.catalogPackage.id] ?: request.appSet
                    val title = packageAppSet.legacyPackageNames[artifact.resolved.catalogPackage.id]
                        ?: artifact.resolved.catalogPackage.name.replace(Regex("[^A-Za-z0-9_-]"), "")
                    val nested = packageZip(artifact, title, nestedDirectory)
                    finalZip.file("AppSet/${packageAppSet.name}/$title.zip", nested)
                    packageSizes.append(title).append('=').append(nested.length()).append('\n')
                    packageRows.getOrPut(packageAppSet.name) { mutableListOf() } +=
                        Triple(title, nested.length(), artifact.descriptor.defaultPartition)
                    nested.delete()
                }
                nestedDirectory.delete()
                finalZip.text("common/file_size.txt", packageSizes.toString())
                finalZip.text("common/install.sh", finalInstaller(packageRows))
                finalZip.text("afzc/nikgapps.config", config(request, artifacts))
                finalZip.text("nikgapps/build-manifest.json", manifest(request, artifacts))
                finalZip.text("zip_name.txt", output.nameWithoutExtension)
                finalZip.text("META-INF/com/google/android/updater-script", "#MAGISK\n")
            }
            if (!part.renameTo(output)) error("Cannot publish ${output.name}")
            return output
        } catch (e: Exception) { part.delete(); output.delete(); throw e }
        finally { nestedDirectory.listFiles()?.forEach { it.delete() }; nestedDirectory.delete() }
    }

    private fun packageZip(artifact: ValidatedArtifact, title: String, directory: File): File {
        val nested = File(directory, "${artifact.resolved.catalogPackage.id}.zip")
        ZipOutputStream(nested.outputStream().buffered()).use { out ->
            ZipFile(artifact.file).use { source ->
                artifact.descriptor.files.sortedBy { it.path }.forEach { payload ->
                    val installPath = PackageZipValidator().installPath(payload.path, artifact.descriptor.defaultPartition)
                    val encoded = encodeBuilderPath(installPath)
                    source.getInputStream(source.getEntry("payload/default/${payload.path}")).use { out.stream(encoded, it) }
                }
            }
            out.text("installer.sh", packageInstaller(title, artifact, artifact.descriptor.files.sumOf { it.size }))
            out.text("uninstaller.sh", packageUninstaller(title, artifact))
        }
        return nested
    }
    private fun encodeBuilderPath(path: String): String {
        val parts = path.split('/'); return "___${parts.dropLast(1).joinToString("___")}/${parts.last()}"
    }
    private fun packageInstaller(title: String, a: ValidatedArtifact, size: Long) = """
        #!/sbin/sh
        configValue="${'$'}1"
        nikgapps_config_file_name="${'$'}2"
        install_partition="${'$'}3"
        pkg_required_size="${'$'}4"
        part_size="${'$'}5"
        make_dir() {
          mkdir -p "${'$'}install_partition/${'$'}1"
          set_perm 1000 1000 0755 "${'$'}install_partition/${'$'}1"
        }
        default_partition="${a.descriptor.defaultPartition}"
        title="$title"
        package_title="$title"
        pkg_size="$size"
        package_name="${a.descriptor.packageName.orEmpty()}"
        remove_aosp_apps_from_rom="
        ${a.descriptor.removeFiles.joinToString("\n")}
        "
        file_list="
        ${a.descriptor.files.joinToString("\n") { encodeBuilderPath(PackageZipValidator().installPath(it.path, a.descriptor.defaultPartition)) }}
        "
        find_install_mode
    """.trimIndent() + "\n"
    private fun packageUninstaller(title: String, a: ValidatedArtifact) = """
        #!/sbin/sh
        uninstall_package() {
          for i in ${'$'}file_list; do uninstall_file "${'$'}i" "${'$'}package_title"; done
        }
        title="$title"
        package_title="$title"
        package_name="${a.descriptor.packageName.orEmpty()}"
        file_list="
        ${a.descriptor.files.joinToString("\n") { encodeBuilderPath(PackageZipValidator().installPath(it.path, a.descriptor.defaultPartition)) }}
        "
        uninstall_package
    """.trimIndent() + "\n"
    private fun finalInstaller(groups: Map<String, List<Triple<String, Long, String>>>) = buildString {
        append("#!/sbin/sh\n# Shell Script EDIFY Replacement\n\n")
        groups.forEach { (appSet, rows) ->
            append("$appSet=\"\n")
            rows.sortedByDescending { it.second }.forEach { append("${it.first},${it.second},${it.third}\n") }
            append("\"\n\n")
        }
        groups.keys.forEach { appSet -> append("install_app_set \"$appSet\" \"$$appSet\" \".zip\"\n") }
        append("set_progress 1.00\nexit_install\n")
    }
    private fun config(r: BuildRequest, artifacts: List<ValidatedArtifact>) = buildString {
        append("# Generated NikGapps configuration; hidden dependencies are intentionally omitted.\n")
        append("AppSet=${r.appSet.name}\n")
        artifacts.filterNot { it.resolved.hidden }.forEach { append("${it.resolved.catalogPackage.name}=1\n") }
    }
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
    companion object { val REQUIRED_ASSETS = setOf("META-INF/com/google/android/update-binary", "common/functions.sh",
        "common/nikgapps_functions.sh", "common/addon.sh", "common/header.sh", "common/mount.sh", "common/unmount.sh") }
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

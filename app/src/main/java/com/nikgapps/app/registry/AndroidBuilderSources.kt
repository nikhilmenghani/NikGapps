package com.nikgapps.app.registry

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request

class AndroidBuilderAssetSource(private val context: Context,
    private val builderAssets: Map<String, BuilderAsset>) : BuilderAssetSource {
    override fun assets(): Map<String, ByteArray> = RegistryZipAssembler.REQUIRED_ASSETS
        .associateWith(::registryAsset)
    private fun registryAsset(name: String): ByteArray {
        val metadata = builderAssets[name] ?: error("Missing builder asset metadata for '$name'")
        val directory = File(context.cacheDir, "nikgapps/builder-assets").apply { mkdirs() }
        val target = File(directory, metadata.sha256)
        if (target.isFile && target.length() == metadata.size && ArtifactDownloader.sha256(target) == metadata.sha256)
            return target.readBytes()
        val part = File(directory, "${metadata.sha256}.part")
        try {
            OkHttpClient().executeRegistryRequest(Request.Builder().url(metadata.url).build()) { response ->
                part.outputStream().use { output -> response.body.byteStream().use { it.copyTo(output) } }
            }
            require(part.length() == metadata.size) { "Size mismatch for builder asset '$name'" }
            require(ArtifactDownloader.sha256(part) == metadata.sha256) { "Checksum mismatch for builder asset '$name'" }
            if (!part.renameTo(target)) error("Cannot cache builder asset '$name'")
            return target.readBytes()
        } finally { part.delete() }
    }
}

class DeviceArtifactFactory(private val context: Context) {
    fun create(resolved: ResolvedPackage, directory: File): ValidatedArtifact {
        val packageName = resolved.version.packageName
            ?: throw IllegalArgumentException("'${resolved.catalogPackage.name}' has no Android package to read from the device")
        @Suppress("DEPRECATION")
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            context.packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        else context.packageManager.getApplicationInfo(packageName, 0)
        val sources = listOfNotNull(info.sourceDir) + info.splitSourceDirs.orEmpty()
        if (sources.isEmpty()) error("No APK files found on the device for $packageName")
        val primary = resolved.version.apk?.path ?: "priv-app/${resolved.catalogPackage.name}/${resolved.catalogPackage.name}.apk"
        val parent = primary.substringBeforeLast('/', "")
        val payloads = sources.mapIndexed { index, source ->
            val input = File(source)
            if (!input.canRead()) error("Cannot read ${input.name}; grant root access or use the catalog package")
            val path = if (index == 0) primary else "$parent/${input.name}"
            Triple(path, input, sha256(input))
        }
        directory.mkdirs()
        val output = File(directory, "device-${resolved.catalogPackage.id}.zip")
        val descriptorJson = buildJsonObject {
            put("schemaVersion", 1); put("id", resolved.catalogPackage.id); put("packageName", packageName)
            put("defaultPartition", resolved.version.defaultPartition)
            put("apk", buildJsonObject { put("path", primary); put("replaceable", true) })
            put("files", buildJsonArray { payloads.forEach { (path, file, hash) -> add(buildJsonObject {
                put("path", path); put("archivePath", encode(path)); put("installPath", "${resolved.version.defaultPartition}/$path")
                put("sha256", hash); put("size", file.length()); put("type", if (path == primary) "primaryApk" else "splitApk")
            }) } })
            put("install", buildJsonObject {
                put("format", "nikgapps-package-v1"); put("title", resolved.catalogPackage.name)
                put("packageTitle", resolved.catalogPackage.name); put("payloadSize", payloads.sumOf { it.second.length() })
                put("removeFiles", buildJsonArray {}); put("removeOverlays", buildJsonArray {})
                put("privilegedPermissions", buildJsonArray {}); put("cleanFlashOnly", false); put("addonIndex", "09")
            })
        }.toString()
        ZipOutputStream(output.outputStream()).use { zip ->
            payloads.forEach { (path, file, _) -> zip.putNextEntry(ZipEntry(encode(path))); file.inputStream().use { it.copyTo(zip) }; zip.closeEntry() }
            zip.putNextEntry(ZipEntry("installer.sh")); zip.write("find_install_mode\n".toByteArray()); zip.closeEntry()
            zip.putNextEntry(ZipEntry("uninstaller.sh")); zip.write("uninstall_package\n".toByteArray()); zip.closeEntry()
            zip.putNextEntry(ZipEntry("package.json")); zip.write(descriptorJson.toByteArray()); zip.closeEntry()
        }
        val checksum = sha256(output)
        val deviceResolved = resolved.copy(versionKey = "device", channel = resolved.channel,
            version = resolved.version.copy(versionName = "device", artifact = Artifact(output.toURI().toString(), checksum, output.length())))
        return ValidatedArtifact(deviceResolved, output, PackageZipValidator().validate(output, deviceResolved))
    }
    private fun sha256(file: File): String { val d = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val b = ByteArray(65536); while (true) { val n=input.read(b); if(n<0) break; d.update(b,0,n) } }
        return d.digest().joinToString("") { "%02x".format(it) }
    }
    private fun encode(path: String): String { val parts = path.split('/'); return "___${parts.dropLast(1).joinToString("___")}/${parts.last()}" }
}

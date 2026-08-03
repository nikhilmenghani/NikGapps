package com.nikgapps.app.registry

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.nikgapps.R
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.*

class AndroidBuilderAssetSource(private val context: Context) : BuilderAssetSource {
    override fun assets(): Map<String, ByteArray> = mapOf(
        "META-INF/com/google/android/update-binary" to recoveryBinary().toByteArray(),
        "common/functions.sh" to raw(R.raw.variables),
        "common/nikgapps_functions.sh" to raw(R.raw.install_package),
        "common/addon.sh" to (raw(R.raw.addon_header) + raw(R.raw.addon_tail)),
        "common/header.sh" to raw(R.raw.addon_header),
        "common/mount.sh" to raw(R.raw.mount),
        "common/unmount.sh" to "#!/sbin/sh\n".toByteArray()
    )
    private fun raw(id: Int) = context.resources.openRawResource(id).use { it.readBytes() }
    private fun recoveryBinary() = """#!/sbin/sh
        OUTFD=${'$'}2
        ZIPFILE=${'$'}3
        TMPDIR=/tmp/nikgapps
        rm -rf "${'$'}TMPDIR"; mkdir -p "${'$'}TMPDIR"
        unzip -o "${'$'}ZIPFILE" 'common/*' 'afzc/*' 'AppSet/*' -d "${'$'}TMPDIR" >/dev/null || exit 1
        COMMONDIR="${'$'}TMPDIR/common"; export COMMONDIR ZIPFILE OUTFD
        . "${'$'}COMMONDIR/functions.sh"
        . "${'$'}COMMONDIR/nikgapps_functions.sh"
        . "${'$'}COMMONDIR/install.sh"
    """.trimIndent() + "\n"
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
            if (!input.canRead()) error("Cannot read ${input.name}; grant root access or use GitLab")
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

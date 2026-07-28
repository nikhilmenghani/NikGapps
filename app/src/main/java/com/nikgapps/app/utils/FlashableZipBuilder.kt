package com.nikgapps.app.utils

import android.content.Context
import android.content.ContentValues
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.nikgapps.app.data.BuildProject
import com.nikgapps.app.data.SupportedApp
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ZipBuildProgress(
    val completed: Int,
    val total: Int,
    val message: String
) {
    val fraction: Float
        get() = if (total == 0) 0f else completed.toFloat() / total
}

sealed interface ZipBuildResult {
    data class Success(val location: String) : ZipBuildResult
    data class Failure(val message: String) : ZipBuildResult
}

class FlashableZipBuilder(private val context: Context) {
    suspend fun build(
        project: BuildProject,
        apps: List<SupportedApp>,
        onProgress: suspend (ZipBuildProgress) -> Unit
    ): ZipBuildResult = withContext(Dispatchers.IO) {
        if (apps.isEmpty()) {
            return@withContext ZipBuildResult.Failure("Select at least one app to build a ZIP.")
        }

        val outputDirectory = File(context.cacheDir, "zip-builds")
        outputDirectory.mkdirs()
        val safeProjectName = project.name
            .replace(Regex("[^A-Za-z0-9._-]+"), "-")
            .trim('-')
            .ifEmpty { "project" }
        val outputFile = File(
            outputDirectory,
            "NikGapps-$safeProjectName-${project.androidVersion.apiLevel}-" +
                "${project.architecture.value}.zip"
        )

        val temporaryFiles = mutableListOf<File>()
        try {
            ZipOutputStream(outputFile.outputStream().buffered()).use { zip ->
                addTextEntry(zip, "META-INF/com/google/android/update-binary", UPDATE_BINARY)
                addTextEntry(zip, "META-INF/com/google/android/updater-script", "#MAGISK\n")
                addTextEntry(zip, "nikgapps/project.json", projectMetadata(project, apps))

                apps.forEachIndexed { index, app ->
                    onProgress(
                        ZipBuildProgress(index, apps.size, "Reading ${app.name} from device…")
                    )
                    val applicationInfo = findApplicationInfo(app.packageName)
                        ?: return@withContext failure(
                            outputFile,
                            temporaryFiles,
                            "${app.name} (${app.packageName}) is not installed or visible."
                        )
                    val apkPaths = listOfNotNull(applicationInfo.sourceDir) +
                        applicationInfo.splitSourceDirs.orEmpty()
                    if (apkPaths.isEmpty()) {
                        return@withContext failure(
                            outputFile,
                            temporaryFiles,
                            "No APK files were found for ${app.name}."
                        )
                    }

                    val appDirectory = app.name.replace(Regex("[^A-Za-z0-9._-]+"), "")
                    apkPaths.forEachIndexed { apkIndex, apkPath ->
                        val readableApk = readableFile(apkPath, temporaryFiles)
                            ?: return@withContext failure(
                                outputFile,
                                temporaryFiles,
                                "Unable to read ${File(apkPath).name} for ${app.name}. Root access may be required."
                            )
                        val apkName = if (apkIndex == 0) "base.apk" else File(apkPath).name
                        addFileEntry(
                            zip,
                            "product/priv-app/$appDirectory/$apkName",
                            readableApk
                        )
                    }
                    onProgress(
                        ZipBuildProgress(index + 1, apps.size, "Added ${app.name}")
                    )
                }
            }
            onProgress(
                ZipBuildProgress(apps.size, apps.size, "Saving ZIP to Downloads…")
            )
            val publishedLocation = publishToDownloads(outputFile)
                ?: return@withContext failure(
                    outputFile,
                    temporaryFiles,
                    "The ZIP was built but could not be saved to Downloads."
                )
            outputFile.delete()
            temporaryFiles.forEach(File::delete)
            ZipBuildResult.Success(publishedLocation)
        } catch (error: Exception) {
            failure(
                outputFile,
                temporaryFiles,
                error.message ?: "Failed to create the flashable ZIP."
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun findApplicationInfo(packageName: String): ApplicationInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                context.packageManager.getApplicationInfo(packageName, 0)
            }
        }.getOrNull()
    }

    private fun readableFile(path: String, temporaryFiles: MutableList<File>): File? {
        val source = File(path)
        if (source.canRead()) return source

        val temporaryFile = File.createTempFile("nikgapps-apk-", ".apk", context.cacheDir)
        val copyResult = Shell.cmd(
            "cp ${shellQuote(path)} ${shellQuote(temporaryFile.absolutePath)}",
            "chmod 0644 ${shellQuote(temporaryFile.absolutePath)}"
        ).exec()
        return if (copyResult.isSuccess && temporaryFile.canRead()) {
            temporaryFiles += temporaryFile
            temporaryFile
        } else {
            temporaryFile.delete()
            null
        }
    }

    private fun addFileEntry(zip: ZipOutputStream, path: String, source: File) {
        zip.putNextEntry(ZipEntry(path))
        FileInputStream(source).use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun addTextEntry(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray())
        zip.closeEntry()
    }

    private fun projectMetadata(
        project: BuildProject,
        apps: List<SupportedApp>
    ): String = JSONObject()
        .put("name", project.name)
        .put("androidApi", project.androidVersion.apiLevel)
        .put("architecture", project.architecture.value)
        .put("source", "device")
        .put("packages", JSONArray(apps.map { it.packageName }))
        .toString(2)

    @Suppress("DEPRECATION")
    private fun publishToDownloads(source: File): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "NikGapps"
            )
            if (!directory.exists() && !directory.mkdirs()) return null
            val destination = File(directory, source.name)
            source.copyTo(destination, overwrite = true)
            return destination.absolutePath
        }

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, source.name)
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/NikGapps"
            )
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().buffered().use { input -> input.copyTo(output) }
            } ?: error("Unable to open Downloads output")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "Downloads/NikGapps/${source.name}"
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    private fun failure(
        outputFile: File,
        temporaryFiles: List<File>,
        message: String
    ): ZipBuildResult.Failure {
        outputFile.delete()
        temporaryFiles.forEach(File::delete)
        return ZipBuildResult.Failure(message)
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"

    private companion object {
        val UPDATE_BINARY = """
            #!/sbin/sh
            OUTFD=${'$'}2
            ZIPFILE=${'$'}3
            ui_print() {
              echo "ui_print ${'$'}1" > /proc/self/fd/${'$'}OUTFD
              echo "ui_print" > /proc/self/fd/${'$'}OUTFD
            }
            abort() {
              ui_print "Error: ${'$'}1"
              exit 1
            }

            ui_print "Installing device-built NikGapps package"
            WORKDIR=/tmp/nikgapps
            rm -rf "${'$'}WORKDIR"
            mkdir -p "${'$'}WORKDIR" || abort "Cannot create temporary directory"
            unzip -o "${'$'}ZIPFILE" "product/*" -d "${'$'}WORKDIR" >/dev/null ||
              abort "Cannot extract package"

            mount /product 2>/dev/null
            if [ -d /product/priv-app ]; then
              PRODUCT=/product
            else
              mount /system 2>/dev/null
              [ -d /system/system ] && SYSTEM=/system/system || SYSTEM=/system
              PRODUCT="${'$'}SYSTEM/product"
            fi
            [ -d "${'$'}PRODUCT" ] || abort "Product partition is unavailable"
            cp -rf "${'$'}WORKDIR/product/." "${'$'}PRODUCT/" ||
              abort "Cannot copy apps to product partition"
            find "${'$'}PRODUCT/priv-app" -type d -exec chmod 0755 {} \;
            find "${'$'}PRODUCT/priv-app" -type f -name "*.apk" -exec chmod 0644 {} \;
            ui_print "Installation complete"
            exit 0
        """.trimIndent()
    }
}

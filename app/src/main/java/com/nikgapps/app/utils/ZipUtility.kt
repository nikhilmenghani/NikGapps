package com.nikgapps.app.utils

import android.os.Build
import com.jaredrummler.android.device.DeviceName
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtility {
    suspend fun extractZip(
        progressLogViewModel: ProgressLogViewModel,
        zipFilePath: String,
        includeExtn: List<String> = emptyList(),
        extractNestedZips: Boolean = false,
        deleteZipAfterExtract: Boolean = false,
        cleanExtract: Boolean = false,
        progressCallback: (String) -> Unit
    ): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val zipFile = File(zipFilePath)
        val outputDir = File(zipFile.parentFile, zipFile.nameWithoutExtension)
        try {
            // Never delete another operation's extraction. Install callers use private workspaces.
            SafeZipExtractor.extract(zipFile, outputDir, extractNestedZips,
                include = { name -> includeExtn.isEmpty() || includeExtn.any { name.contains(it, true) } },
                progress = progressCallback)
            if (deleteZipAfterExtract) check(zipFile.delete())
            true
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) { progressLogViewModel.addLog("Extraction failed: ${error.message}"); false }
    }

    fun saveLogs(filePath: String): Boolean {
        val zipFile = File(filePath)
        val parentDir = zipFile.parentFile

        try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // Add specified files to the zip
                val filesToZip = listOf("NikGapps_logs.log") // Specify the files you want to zip
                filesToZip.forEach { fileName ->
                    val fileToZip = File(parentDir, fileName)
                    if (fileToZip.exists()) {
                        FileInputStream(fileToZip).use { fis ->
                            zos.putNextEntry(ZipEntry(fileToZip.name))
                            fis.copyTo(zos)
                            zos.closeEntry()
                        }
                    }
                }

                // Write new text files with device information
                val deviceInfo = """
                    Device Name: ${DeviceName.getDeviceName(Build.DEVICE, Build.MODEL)}
                    Device Code: ${Build.DEVICE}
                    Android Version: ${Build.VERSION.RELEASE}
                """.trimIndent()
                zos.putNextEntry(ZipEntry("device_info.txt"))
                zos.write(deviceInfo.toByteArray())
                zos.closeEntry()

                // Execute commands and store their output in the zip file
                val commands = listOf("df", "cat /proc/mounts")
                commands.forEach { command ->
                    val process = Runtime.getRuntime().exec(command)
                    val output = process.inputStream.bufferedReader().use { it.readText() }
                    val fileName = command
                        .trim()
                        .replace(" ", "")
                        .replace("-", "_")
                        .replace("/", "_")
                    zos.putNextEntry(ZipEntry("${fileName}.txt"))
                    zos.write(output.toByteArray())
                    zos.closeEntry()
                }
            }
            return true
        } catch (e: IOException) {
            return false
        }
    }
}

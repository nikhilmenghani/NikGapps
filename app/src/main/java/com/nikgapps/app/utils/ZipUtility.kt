package com.nikgapps.app.utils

import android.os.Build
import android.widget.Toast
import com.nikgapps.app.data.model.LogManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.ZipEntry
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
    ): Boolean = coroutineScope {
        try {
            val zipFile = File(zipFilePath)
            val outputDir = File(zipFile.parentFile?.absolutePath, zipFile.nameWithoutExtension)

            if(cleanExtract && outputDir.exists()) {
                outputDir.deleteRecursively()
                outputDir.mkdirs()
            }

            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val deferreds = mutableListOf<Deferred<Boolean>>()
            if (zipFile.extension == "zip") {
                progressLogViewModel.addLog("Extracting: ${zipFile.name}")
            }
            ZipInputStream(zipFile.inputStream()).use { zipStream ->
                var entry = zipStream.nextEntry
                while (entry != null) {
                    val fileName = entry.name

                    if (includeExtn.isEmpty() || includeExtn.any {
                            fileName.contains(it, ignoreCase = true)
                        }) {
                        val extractedFile = File(outputDir, fileName)
                        if (entry.isDirectory) {
                            extractedFile.mkdirs()
                        } else {
                            progressCallback("Extracting $fileName...")
                            extractedFile.parentFile?.mkdirs()
                            FileOutputStream(extractedFile).use { outputStream ->
                                zipStream.copyTo(outputStream)
                            }

                            // If the extracted file is a zip and the extractNestedZips flag is true, extract it asynchronously
                            if (extractNestedZips && extractedFile.extension == "zip") {
                                val nestedOutputDir = File(
                                    extractedFile.parentFile,
                                    extractedFile.nameWithoutExtension
                                )
                                if (!nestedOutputDir.exists()) {
                                    nestedOutputDir.mkdirs()
                                }
                                val deferred = async(Dispatchers.IO) {
                                    extractZip(
                                        progressLogViewModel,
                                        extractedFile.absolutePath,
                                        extractNestedZips = false,
                                        deleteZipAfterExtract = true,
                                        progressCallback = progressCallback
                                    )
                                }
                                deferreds.add(deferred)
                            }
                        }
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }

            // Await all nested extraction tasks to complete
            val nestedResults = deferreds.awaitAll()
            if (nestedResults.any { !it }) {
                return@coroutineScope false
            }
            if (deleteZipAfterExtract) {
                zipFile.delete()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveLogs(filename: String) : Boolean{
        val file = File(LogManager.APP_LOGS_FILE_NAME)
        val parentDir = file.parentFile
        val zipFile = File(parentDir, filename)

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
            Device Name: ${Build.MODEL}
            Device Code: ${Build.DEVICE}
            Android Version: ${Build.VERSION.RELEASE}
        """.trimIndent()
                zos.putNextEntry(ZipEntry("device_info.txt"))
                zos.write(deviceInfo.toByteArray())
                zos.closeEntry()

                // Execute commands and store their output in the zip file
                val commands = listOf("uname -a", "df -h")
                commands.forEach { command ->
                    val process = Runtime.getRuntime().exec(command)
                    val output = process.inputStream.bufferedReader().use { it.readText() }
                    zos.putNextEntry(ZipEntry("${command.replace(" ", "_")}.txt"))
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

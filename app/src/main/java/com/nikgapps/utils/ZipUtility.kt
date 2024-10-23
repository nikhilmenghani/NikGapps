package com.nikgapps.utils

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object ZipUtility {
    fun extractZip(zipFilePath: String, outputDirPath: String, includeExtn: List<String> = emptyList(), extractNestedZips: Boolean = false): Boolean {
        return try {
            val zipFile = File(zipFilePath)
            val outputDir = File(outputDirPath)

            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            ZipInputStream(zipFile.inputStream()).use { zipStream ->
                var entry = zipStream.nextEntry
                while (entry != null) {
                    val fileName = entry.name

                    if (includeExtn.isEmpty() || includeExtn.any { fileName.contains(it, ignoreCase = true) }) {
                        val extractedFile = File(outputDir, fileName)
                        if (entry.isDirectory) {
                            extractedFile.mkdirs()
                        } else {
                            extractedFile.parentFile?.mkdirs()
                            FileOutputStream(extractedFile).use { outputStream ->
                                zipStream.copyTo(outputStream)
                            }

                            // If the extracted file is a zip and the extractNestedZips flag is true, extract it
                            if (extractNestedZips && extractedFile.extension == "zip") {
                                val nestedOutputDir = File(extractedFile.parentFile, extractedFile.nameWithoutExtension)
                                if (!nestedOutputDir.exists()) {
                                    nestedOutputDir.mkdirs()
                                }
                                val nestedExtractSuccessful = extractZip(extractedFile.absolutePath, nestedOutputDir.absolutePath, extractNestedZips = true)
                                if (!nestedExtractSuccessful) {
                                    Log.e("ZipUtility", "Failed to extract nested zip file ${extractedFile.name}")
                                } else {
                                    extractedFile.delete()
                                    Log.d("ZipUtility", "Nested zip file ${extractedFile.name} extracted successfully into ${nestedOutputDir.path}")
                                }
                            }
                        }
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun extractNestedZips(directory: File): Boolean {
        var allSuccess = true
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                // Recursively call extractNestedZips for subdirectories
                val success = extractNestedZips(file)
                if (!success) {
                    allSuccess = false
                }
            } else if (file.extension == "zip") {
                // Create a folder with the same name as the zip file (without extension)
                val extractDir = File(file.parentFile, file.nameWithoutExtension)
                if (!extractDir.exists()) {
                    extractDir.mkdirs()
                }
                // Extract zip into the newly created directory
                val nestedExtractSuccessful = extractZip(file.absolutePath, extractDir.absolutePath)
                if (nestedExtractSuccessful) {
                    Log.d("ZipUtility", "Nested zip file ${file.name} extracted successfully into ${extractDir.path}")
                } else {
                    Log.e("ZipUtility", "Failed to extract nested zip file ${file.name}")
                    allSuccess = false
                }
            }
        }
        return allSuccess
    }
}

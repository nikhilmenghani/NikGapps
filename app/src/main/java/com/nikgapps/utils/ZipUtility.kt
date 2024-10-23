package com.nikgapps.utils

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object ZipUtility {
    /**
     * Extracts a zip file to the specified output directory.
     *
     * @param zipFilePath The path of the zip file to be extracted.
     * @param outputDirPath The directory path where the contents should be extracted.
     * @param includeExtn A list of file extensions or strings that should be included during extraction (e.g., ".zip", ".apk"). If empty, extracts all files.
     * @return True if the extraction was successful, False otherwise.
     */
    fun extractZip(zipFilePath: String, outputDirPath: String, includeExtn: List<String> = emptyList()): Boolean {
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
}

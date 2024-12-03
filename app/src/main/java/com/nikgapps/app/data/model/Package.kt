package com.nikgapps.app.data.model

data class Package(
    val packageName: String = "",
    val partition: String = "",
    val title: String = "",
    val packageTitle: String = "",
    val packageSize: String = "",
    val appType: String = "",
    val cleanFlash: String = "",
    val sourceDirectory: String = "",
    val fileList: List<String> = emptyList(),
    val overlayList: List<String> = emptyList(),
    val otherFilesList: List<String> = emptyList(),
    val removeAospAppsList: List<String> = emptyList(),
) {
    fun addFile(fileName: String): Package {
        val updatedFileList = fileList.toMutableList().apply { add(fileName) }
        return copy(fileList = updatedFileList)
    }
}

fun Package.getInstallScript(baseScript: String): String {
    return buildString {
        appendLine("#!/bin/sh")
        appendLine()
        appendLine(". /sdcard/NikGapps/NikGapps_flags.log")
        appendLine()
        appendLine("# Initializing Variables")
        appendLine("title=\"$packageTitle\"")
        appendLine("pkg_name=\"$packageName\"")
        appendLine("pkg_size=\"$packageSize\"")
        appendLine("default_partition=\"$partition\"")
        appendLine("clean_flash=\"$cleanFlash\"")
        appendLine("app_type=\"$appType\"")
        appendLine("source_directory=\"$sourceDirectory\"")
        appendLine()
        appendLine("remove_aosp_apps_from_rom=\"\n")
        removeAospAppsList.forEach { appendLine(it) }
        appendLine("\"")
        appendLine()
        appendLine("file_list=\"")
        fileList.forEach { appendLine(it) }
        appendLine("\"")
        appendLine()
        append(baseScript)
    }
}
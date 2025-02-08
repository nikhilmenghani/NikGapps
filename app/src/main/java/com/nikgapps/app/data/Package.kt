package com.nikgapps.app.data

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
    val addonIndex: String = "05"
) {
    fun addFile(fileName: String): Package {
        val updatedFileList = fileList.toMutableList().apply { add(fileName) }
        return copy(fileList = updatedFileList)
    }

    fun getInstallScript(baseScript: String): String {
        return buildString {
            appendLine("#!/bin/sh")
            appendLine()
            appendLine("cd \"\$(dirname \"\$0\")\"")
            appendLine("source ./NikGapps_flags.log")
            appendLine("source ./variables.sh")
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
            appendLine("remove_aosp_apps_from_rom=\"")
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

    fun getAddonScript(addonHeader: String, addonCore: String, addonTail: String): String {
        return buildString{
            appendLine("#!/sbin/sh")
            appendLine("#")
            appendLine("# ADDON_VERSION=3")
            appendLine("#")
            appendLine("# Addon.d script created from AFZC tool by Nikhil Menghani")
            appendLine("#")
            appendLine()
            appendLine("package_title=\"$packageTitle\"")
            appendLine()
            appendLine(addonHeader)
            appendLine()
            appendLine(addonCore)
            appendLine(addonTail)
        }
    }
}
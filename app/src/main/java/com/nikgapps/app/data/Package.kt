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
    private fun validate() {
        require(packageTitle.matches(Regex("[A-Za-z0-9][A-Za-z0-9_.-]{0,127}")) && ".." !in packageTitle) { "Invalid package title" }
        require(partition in setOf("system", "system_ext", "product", "vendor")) { "Invalid partition" }
        require(appType in setOf("", "app", "priv-app")) { "Invalid app type" }
        require(addonIndex.matches(Regex("[0-9]{2}"))) { "Invalid addon index" }
        (fileList + removeAospAppsList).forEach {
            val decoded = it.removePrefix("___").replace("___", "/")
            require(it.isNotBlank() && it.matches(Regex("[A-Za-z0-9_./+-]+")) &&
                !decoded.startsWith('/') && decoded.split('/').none { part -> part == ".." || part == "." }) { "Invalid package path" }
        }
    }
    fun addFile(fileName: String): Package {
        val updatedFileList = fileList.toMutableList().apply { add(fileName) }
        return copy(fileList = updatedFileList)
    }

    fun getInstallScript(baseScript: String): String {
        validate()
        fun quoted(value: String) = com.nikgapps.app.utils.root.ShellCommands.quote(value)
        return buildString {
            appendLine("#!/bin/sh")
            appendLine()
            appendLine("cd \"\$(dirname \"\$0\")\"")
            appendLine("source ./NikGapps_flags.log")
            appendLine("source ./variables.sh")
            appendLine()
            appendLine("# Initializing Variables")
            appendLine("title=${quoted(packageTitle)}")
            appendLine("pkg_name=${quoted(packageName)}")
            appendLine("pkg_size=${quoted(packageSize)}")
            appendLine("default_partition=${quoted(partition)}")
            appendLine("clean_flash=${quoted(cleanFlash)}")
            appendLine("app_type=${quoted(appType)}")
            appendLine("source_directory=${quoted(sourceDirectory)}")
            appendLine()
            appendLine("remove_aosp_apps_from_rom=${quoted(removeAospAppsList.joinToString("\n"))}")
            appendLine()
            appendLine("file_list=${quoted(fileList.joinToString("\n"))}")
            appendLine()
            append(baseScript)
        }
    }

    fun getAddonScript(addonHeader: String, addonCore: String, addonTail: String): String {
        validate()
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

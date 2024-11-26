package com.nikgapps.app.utils

import com.nikgapps.app.data.model.AppSet
import com.nikgapps.app.data.model.Package
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel
import java.io.File

object BuildUtility {
    suspend fun scanForApps(
        progressLogViewModel: ProgressLogViewModel,
        directory: String
    ): List<AppSet> {
        val appSets = mutableListOf<AppSet>()

        val dir = File(directory)
        if (!dir.exists() || !dir.isDirectory) {
            progressLogViewModel.addLog("Directory does not exist or is not a directory: $directory")
            return appSets
        }

        dir.walkTopDown().forEach { file ->
            when {
                file.isDirectory && file.parentFile?.name == "AppSet" -> {
                    val appSet = processAppSet(file)
                    appSets.add(appSet)
                }
            }
        }

        return appSets
    }

    private suspend fun processAppSet(directory: File): AppSet {
        val packages = mutableListOf<Package>()

        directory.listFiles()?.forEach { subFile ->
            if (subFile.isDirectory) {
                packages.add(processPackage(subFile))
            }
        }

        return AppSet(directory.name, packages)
    }

    private suspend fun processPackage(directory: File): Package {
        val files = directory.listFiles()?.filter { it.isFile }?.map { it.name } ?: emptyList()
        val packageInfo = mutableMapOf<String, String>()
        val fileList = mutableListOf<String>()
        val removeAospAppsList = mutableListOf<String>()
        val overlayList = mutableListOf<String>()
        val otherFilesList = mutableListOf<String>()

        files.forEach { file ->
            if (file.equals("installer.sh", ignoreCase = true)) {
                var isFileList = false
                var isAospApps = false
                File(directory, file).readLines().forEach { line ->
                    when {
                        line.startsWith("title=") -> packageInfo["title"] = line.substringAfter("title=\"").trim().dropLast(1)
                        line.startsWith("package_title=") -> packageInfo["packageTitle"] = line.substringAfter("package_title=\"").trim().dropLast(1)
                        line.startsWith("package_name=") -> packageInfo["packageName"] = line.substringAfter("package_name=\"").trim().dropLast(1)
                        line.startsWith("pkg_size=") -> packageInfo["packageSize"] = line.substringAfter("pkg_size=\"").trim().dropLast(1)
                        line.startsWith("default_partition=") -> packageInfo["packagePartition"] = line.substringAfter("default_partition=\"").trim().dropLast(1)
                        line.startsWith("clean_flash=") -> packageInfo["cleanFlash"] = line.substringAfter("clean_flash=\"").trim().dropLast(1)
                        line.startsWith("remove_aosp_apps_from_rom=\"") -> {
                            isAospApps = true
                        }
                        isAospApps -> {
                            if (line.endsWith("\"")) {
                                isAospApps = false
                            }
                            else{
                                removeAospAppsList.add(line.trim())
                            }
                        }
                        line.startsWith("file_list=\"") -> {
                            isFileList = true
                        }
                        isFileList -> {
                            if (line.endsWith("\"")) {
                                isFileList = false
                            } else {
                                if (line.startsWith("___overlay")) {
                                    overlayList.add(line.trim())
                                } else if (line.startsWith("___priv-app") || line.startsWith("___app")) {
                                    fileList.add(line.trim())
                                } else {
                                    otherFilesList.add(line.trim())
                                }
                            }
                        }
                    }
                }
            }
        }

        return Package(
            packageName = packageInfo["packageName"] ?: "test",
            partition = packageInfo["packagePartition"] ?: "",
            title = packageInfo["title"] ?: "",
            packageTitle = packageInfo["packageTitle"] ?: "",
            appType = "appType", // Replace with actual value if needed
            fileList = fileList,
            overlayList = overlayList,
            otherFilesList = otherFilesList,
            removeAospAppsList = removeAospAppsList
        )
    }
}
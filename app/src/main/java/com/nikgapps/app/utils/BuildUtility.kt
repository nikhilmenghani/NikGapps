package com.nikgapps.app.utils

import com.nikgapps.R
import com.nikgapps.app.data.model.AppSet
import com.nikgapps.app.data.model.Package
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel
import com.nikgapps.app.utils.root.RootUtility
import java.io.File

object BuildUtility {
    suspend fun scanForApps(
        progressLogViewModel: ProgressLogViewModel,
        directory: String
    ): List<AppSet> {
        progressLogViewModel.addLog("Building NikGapps...")
        val appSets = mutableListOf<AppSet>()

        val nwe = File(directory).nameWithoutExtension
        val parentDir = File(directory).parentFile
        val dir = parentDir?.resolve(nwe)
        if (dir == null || !dir.exists() || !dir.isDirectory) {
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
        progressLogViewModel.clearLogs()
        progressLogViewModel.addLog("Building Successful!")
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
        var appType = ""

        files.forEach { file ->
            if (file.equals("installer.sh", ignoreCase = true)) {
                var isFileList = false
                var isAospApps = false
                File(directory, file).readLines().forEach { line ->
                    when {
                        line.startsWith("title=") -> packageInfo["title"] = line.extractValue()
                        line.startsWith("package_title=") -> packageInfo["packageTitle"] = line.extractValue()
                        line.startsWith("package_name=") -> packageInfo["packageName"] = line.extractValue()
                        line.startsWith("pkg_size=") -> packageInfo["packageSize"] = line.extractValue()
                        line.startsWith("default_partition=") -> packageInfo["packagePartition"] = line.extractValue()
                        line.startsWith("clean_flash=") -> packageInfo["cleanFlash"] = line.extractValue()
                        line.startsWith("remove_aosp_apps_from_rom=\"") -> isAospApps = true
                        isAospApps -> {
                            if (line.endsWith("\"")) isAospApps = false
                            else removeAospAppsList.add(line.trim())
                        }
                        line.startsWith("file_list=\"") -> isFileList = true
                        isFileList -> {
                            if (line.endsWith("\"")) isFileList = false
                            else {
                                when {
                                    line.startsWith("___overlay") -> overlayList.add(line.trim())
                                    line.startsWith("___priv-app") -> {
                                        fileList.add(line.trim())
                                        appType = "priv-app"
                                    }
                                    line.startsWith("___app") -> {
                                        fileList.add(line.trim())
                                        appType = "app"
                                    }
                                    else -> otherFilesList.add(line.trim())
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
            appType = appType,
            fileList = fileList,
            overlayList = overlayList,
            otherFilesList = otherFilesList,
            removeAospAppsList = removeAospAppsList
        )
    }

    private fun String.extractValue(): String {
        return this.substringAfter("=\"").trim().dropLast(1)
    }

    suspend fun installAppSet(
        progressLogViewModel: ProgressLogViewModel,
        appSet: AppSet,
    ) {
        appSet.packages.forEach { pkg ->
            progressLogViewModel.addLog("Installing ${pkg.packageTitle}")
            progressLogViewModel.addLog("Installing Files:")
            pkg.fileList.forEach { file ->
                progressLogViewModel.addLog("- $file")
            }
            progressLogViewModel.addLog("Installing Overlay:")
            pkg.overlayList.forEach { overlay ->
                progressLogViewModel.addLog("- $overlay")
            }
            progressLogViewModel.addLog("Installing Other Files:")
            pkg.otherFilesList.forEach { otherFile ->
                progressLogViewModel.addLog("- $otherFile")
            }
            progressLogViewModel.addLog("Removing AOSP Apps:")
            pkg.removeAospAppsList.forEach { aospApp ->
                progressLogViewModel.addLog("- Removing $aospApp...")
            }
        }
    }
}
package com.nikgapps.app.utils

import com.nikgapps.app.data.model.AppSet
import com.nikgapps.app.data.model.Package
import com.nikgapps.app.data.model.getInstallScript
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel
import com.nikgapps.app.utils.root.RootManager
import java.io.File

object BuildUtility {
    fun scanForApps(
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

    private fun processAppSet(directory: File): AppSet {
        val packages = mutableListOf<Package>()

        directory.listFiles()?.forEach { subFile ->
            if (subFile.isDirectory) {
                packages.add(processPackage(subFile))
            }
        }

        return AppSet(directory.name, packages)
    }

    private fun processPackage(directory: File): Package {
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
                                    line.startsWith("___overlay") -> fileList.add(line.trim())
                                    line.startsWith("___priv-app") -> {
                                        fileList.add(line.trim())
                                        appType = "priv-app"
                                    }
                                    line.startsWith("___app") -> {
                                        fileList.add(line.trim())
                                        appType = "app"
                                    }
                                    else -> fileList.add(line.trim())
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
            packageSize = packageInfo["packageSize"] ?: "",
            appType = appType,
            cleanFlash = packageInfo["cleanFlash"] ?: "",
            sourceDirectory = directory.absolutePath,
            fileList = fileList,
            overlayList = overlayList,
            otherFilesList = otherFilesList,
            removeAospAppsList = removeAospAppsList
        )
    }

    private fun String.extractValue(): String {
        return this.substringAfter("=\"").trim().dropLast(1)
    }

    fun installAppSet(
        progressLogViewModel: ProgressLogViewModel,
        appSet: AppSet,
        rootManager: RootManager,
        baseScript: String
    ) {
        appSet.packages.forEach { pkg ->
            // Create the script file in /sdcard/NikGapps/pkg.packageTitle.sh
            val scriptFile = File("/sdcard/NikGapps/${pkg.packageTitle}.sh")
            scriptFile.writeText(pkg.getInstallScript(baseScript))
            scriptFile.setExecutable(true)

            // Execute the script using rootManager
            val result = rootManager.executeScript(scriptFile.absolutePath)
            if (result.success) {
                progressLogViewModel.addLog("Successfully installed ${pkg.packageTitle}")
            } else {
                progressLogViewModel.addLog("Failed to install ${pkg.packageTitle}: ${result.output}")
            }

            progressLogViewModel.addLog(result.output)
        }
    }
}
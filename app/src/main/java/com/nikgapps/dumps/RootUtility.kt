package com.nikgapps.dumps

import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object RootUtility {
    /**
     * Copies files from a source directory to a destination directory in the system partition.
     *
     * @param sourceDirPath The path of the source directory containing files to be copied.
     * @param destDirPath The path of the destination directory in the system or product partition.
     * @return True if the files were copied successfully, False otherwise.
     */


    fun mountPartition(partition: String): Boolean {
        return try {
            // Remount the partition with read-write access
            val remountResult = Shell.cmd("mount -o rw,remount $partition").exec()
            if (!remountResult.isSuccess) {
                Log.e("RootUtility", "Failed to remount partition $partition: ${remountResult.out.joinToString("\n")}")
                return false
            }
            Log.d("RootUtility", "Partition $partition remounted successfully: ${remountResult.out.joinToString("\n")}")
            true
        } catch (e: Exception) {
            Log.e("RootUtility", "Exception while remounting partition: ${e.message}")
            false
        }
    }

    fun executeScript(scriptPath: String, vararg args: String): Boolean {
        return try {
            // Load the script from raw resources
            val scriptFile = File(scriptPath)
            if (!scriptFile.exists()) {
                Log.e("RootUtility", "Script file does not exist: $scriptPath")
                return false
            }

            // Change permission to executable
            scriptFile.setExecutable(true)

            // Execute the shell script with root privileges
            val result = Shell.cmd("sh ${scriptFile.absolutePath} ${args.joinToString(" ")}").exec()
            val output = result.out.joinToString("\n")
            Log.d("RootUtility", "Shell script output: \n$output")

            if (result.isSuccess) {
                Log.d("RootUtility", "Script executed successfully: $scriptPath")
                true
            } else {
                Log.e("RootUtility", "Failed to execute script: $output")
                false
            }
        } catch (e: Exception) {
            Log.e("RootUtility", "Exception while executing script: ${e.message}")
            false
        }
    }

    fun copyFilesToSystem(sourceDirPath: String, destDirPath: String): Boolean {
        return try {
            val sourceDir = File(sourceDirPath)

            if (!sourceDir.exists() || !sourceDir.isDirectory) {
                Log.e("RootUtility", "Source directory does not exist or is not a directory")
                return false
            }

            // Remount the partition with read-write access
            val remountResult = Shell.cmd("mount -o rw,remount /product").exec()
            if (!remountResult.isSuccess) {
                Log.e("RootUtility", "Failed to remount partition: ${remountResult.out.joinToString("\n")}")
                return false
            }

            // Create the destination directory if it doesn't exist
            val createDirResult = Shell.cmd("mkdir -p $destDirPath").exec()
            if (!createDirResult.isSuccess) {
                Log.e("RootUtility", "Failed to create directory: ${createDirResult.out.joinToString("\n")}")
                return false
            }

            // Copy files from source to destination
            val copyResult = Shell.cmd("cp -r $sourceDirPath/* $destDirPath").exec()
            if (copyResult.isSuccess) {
                Log.d("RootUtility", "Files copied successfully: ${copyResult.out.joinToString("\n")}")
                return true
            } else {
                Log.e("RootUtility", "Failed to copy files: ${copyResult.out.joinToString("\n")}")
                return false
            }
        } catch (e: Exception) {
            Log.e("RootUtility", "Exception while copying files: ${e.message}")
            false
        }
    }

    // Function to check if the app has root access
    suspend fun hasRootAccess(): Boolean {
        return withContext(Dispatchers.IO) {
            // Close any existing shell to avoid stale results
            Shell.getCachedShell()?.close()

            // Perform a fresh check for root access
            Shell.getShell().isRoot
        }
    }
}
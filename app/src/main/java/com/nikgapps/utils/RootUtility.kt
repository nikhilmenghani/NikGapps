package com.nikgapps.utils

import android.util.Log
import com.topjohnwu.superuser.Shell
import java.io.File

object RootUtility {
    /**
     * Copies files from a source directory to a destination directory in the system partition.
     *
     * @param sourceDirPath The path of the source directory containing files to be copied.
     * @param destDirPath The path of the destination directory in the system or product partition.
     * @return True if the files were copied successfully, False otherwise.
     */
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
}

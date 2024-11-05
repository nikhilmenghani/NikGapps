package com.nikgapps.download

interface DownloadStrategy {
    suspend fun download(downloadUrl: String, destFilePath: String): Boolean
}
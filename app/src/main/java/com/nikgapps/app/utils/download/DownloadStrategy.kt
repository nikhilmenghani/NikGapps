package com.nikgapps.app.utils.download

interface DownloadStrategy {
    suspend fun download(downloadUrl: String, destFilePath: String): Boolean
}
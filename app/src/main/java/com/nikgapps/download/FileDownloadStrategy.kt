package com.nikgapps.download

import com.nikgapps.utils.DownloadUtility

class FileDownloadStrategy : DownloadStrategy {
    override suspend fun download(downloadUrl: String, destFilePath: String): Boolean {
        return DownloadUtility.downloadFile(downloadUrl, destFilePath)
    }
}

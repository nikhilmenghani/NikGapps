package com.nikgapps.download

import com.nikgapps.utils.DownloadUtility

class ApkDownloadStrategy : DownloadStrategy {
    override suspend fun download(downloadUrl: String, destFilePath: String): Boolean {
        return DownloadUtility.downloadApk(downloadUrl, destFilePath)
    }
}
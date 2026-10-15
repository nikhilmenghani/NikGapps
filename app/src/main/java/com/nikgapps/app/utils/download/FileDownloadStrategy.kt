package com.nikgapps.app.utils.download
import java.io.File

class FileDownloadStrategy : DownloadStrategy {
    override suspend fun download(downloadUrl: String, destFilePath: String): Boolean =
        VerifiedDownload().download(downloadUrl, File(destFilePath))
}

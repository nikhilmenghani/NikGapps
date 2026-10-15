package com.nikgapps.app.utils.download
import java.io.File

class ApkDownloadStrategy(private val context: android.content.Context? = null,
    private val expectedVersion: String? = null) : DownloadStrategy {
    override suspend fun download(downloadUrl: String, destFilePath: String): Boolean =
        VerifiedDownload().download(downloadUrl, File(destFilePath), validate = ::validateApk)
    // Legacy synchronous Worker API; the Worker invokes this on its background executor.
    suspend fun downloadApk(url: String, destFilePath: String, onProgressUpdate: ((Float) -> Unit)? = null): Boolean =
        VerifiedDownload().download(url, File(destFilePath), validate = ::validateApk, progress = { onProgressUpdate?.invoke(it) })

    @Suppress("DEPRECATION") // GET_SIGNATURES supports the existing API-24 minimum.
    internal fun validateApk(file: File) {
        java.util.zip.ZipFile(file).use { require(it.getEntry("AndroidManifest.xml") != null) { "Not an APK" } }
        val owner = context ?: return
        val manager = owner.packageManager
        val archive = manager.getPackageArchiveInfo(file.path, android.content.pm.PackageManager.GET_SIGNATURES)
            ?: error("Invalid APK metadata")
        val installed = manager.getPackageInfo(owner.packageName, android.content.pm.PackageManager.GET_SIGNATURES)
        require(archive.packageName == owner.packageName) { "Update belongs to another app" }
        expectedVersion?.takeUnless { it == "update" }?.let { expected ->
            require(archive.versionName == expected || archive.versionName?.startsWith("$expected-") == true) { "Update version mismatch" }
        }
        fun signatures(info: android.content.pm.PackageInfo) = info.signatures.orEmpty().map { it.toCharsString() }.toSet()
        val trusted = signatures(installed)
        require(trusted.isNotEmpty() && signatures(archive) == trusted) { "Update signing certificate mismatch" }
    }
}

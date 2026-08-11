package com.nikgapps.app.registry

import java.io.File

class InsufficientStorage(required: Long, available: Long) : Exception(
    "Insufficient device storage: ${required / 1_048_576} MiB required, ${available / 1_048_576} MiB available")

class RegistryBuildService(private val downloader: ArtifactDownloader,
    private val validator: PackageZipValidator, private val assembler: RegistryZipAssembler) {
    suspend fun build(cacheDirectory: File, outputDirectory: File, metadata: RegistryMetadata,
        request: BuildRequest, onProgress: suspend (DownloadProgress) -> Unit = {}): File {
        val resolved = CatalogResolver(metadata.catalog, metadata.appSets, metadata.release).resolve(request.appSet.id,
            request.selectedIds, request.defaultChannel, request.channelOverrides, request.api, request.architecture)
        val downloadBytes = resolved.sumOf { it.version.artifact.size ?: 0L }
        // Downloads plus nested and final ZIPs can coexist during assembly.
        val required = downloadBytes * 3 + 32L * 1_048_576
        if (cacheDirectory.usableSpace < required) throw InsufficientStorage(required, cacheDirectory.usableSpace)
        val artifacts = resolved.map { pkg ->
            val file = downloader.obtain(pkg, onProgress)
            ValidatedArtifact(pkg, file, validator.validate(file, pkg))
        }
        return assembler.build(outputDirectory, request, artifacts)
    }
}

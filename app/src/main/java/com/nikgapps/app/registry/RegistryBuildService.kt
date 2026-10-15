package com.nikgapps.app.registry

import java.io.File

class InsufficientStorage(required: Long, available: Long) : Exception(
    "Insufficient device storage: ${required / 1_048_576} MiB required, ${available / 1_048_576} MiB available")

class RegistryBuildService(private val downloader: ArtifactDownloader,
    private val validator: PackageZipValidator, private val assembler: RegistryZipAssembler) {
    suspend fun build(cacheDirectory: File, outputDirectory: File, metadata: RegistryMetadata,
        request: BuildRequest, onProgress: suspend (DownloadProgress) -> Unit = {}): File = RegistryCache.withLease {
        val resolved = CatalogResolver(metadata.catalog, metadata.appSets, metadata.release).resolve(request.appSet.id,
            request.selectedIds, request.defaultChannel, request.channelOverrides, request.api, request.architecture)
        StoragePreflight.check(cacheDirectory, resolved.map { it.version.artifact.size })
        StoragePreflight.check(outputDirectory, resolved.map { it.version.artifact.size })
        val artifacts = resolved.map { pkg ->
            val file = downloader.obtain(pkg, onProgress)
            ValidatedArtifact(pkg, file, validator.validate(file, pkg))
        }
        assembler.build(outputDirectory, request, artifacts)
    }
}

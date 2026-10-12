package com.nikgapps.app.registry

import android.content.Context
import com.nikgapps.app.data.BuildProject
import com.nikgapps.app.data.BuildProjectRepository
import java.io.File
import java.util.zip.ZipFile
import org.json.JSONObject

/** Call only after publishing succeeds; the receipt comes from the ZIP actually built. */
fun recordSuccessfulProjectZip(context: Context, snapshot: BuildProject, zip: File) {
    val repository = BuildProjectRepository(context)
    ZipFile(zip).use { archive ->
        val manifest = JSONObject(archive.getInputStream(archive.getEntry("nikgapps/build-manifest.json"))
            .bufferedReader().use { it.readText() })
        val config = archive.getInputStream(archive.getEntry("afzc/nikgapps.config")).bufferedReader().use { it.readText() }
        val version = config.lineSequence().firstOrNull { it.startsWith("Version=") }?.substringAfter('=')?.trim()
        val definitions = manifest.optJSONObject("packageDefinitions")
        val builtRelease = manifest.optString("releaseId").takeIf { it.isNotBlank() }
        repository.mutateProject(snapshot.id) { current ->
            if (current.selectedAppIds != snapshot.selectedAppIds || current.selectedPackageAppSets != snapshot.selectedPackageAppSets ||
                current.androidVersion != snapshot.androidVersion || current.architecture != snapshot.architecture ||
                current.defaultChannel != snapshot.defaultChannel || current.channelOverrides != snapshot.channelOverrides ||
                current.keepAospCounterparts != snapshot.keepAospCounterparts || current.appSources != snapshot.appSources ||
                current.installerSettings != snapshot.installerSettings) return@mutateProject current
            current.copy(lastBuiltReleaseId = builtRelease,
            configVersion = version, dismissedReleaseId = current.dismissedReleaseId?.takeUnless { it == builtRelease }, configSnapshot = config,
            packageDefinitions = definitions?.let { values -> buildMap {
                values.keys().forEach { put(it, values.getString(it)) }
            } }.orEmpty())
        }
    }
}

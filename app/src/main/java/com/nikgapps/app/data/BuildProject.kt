package com.nikgapps.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

const val MAX_PROJECT_NAME_LENGTH = 25
fun projectNameLimit(elite: Boolean): Int = if (elite) MAX_PROJECT_NAME_LENGTH else 20

data class BuildProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val androidVersion: AndroidVersion,
    val architecture: Architecture,
    val selectedAppSetId: String = "core",
    val selectedPackageAppSets: Map<String, String> = emptyMap(),
    val defaultChannel: String = "stable",
    val channelOverrides: Map<String, String> = emptyMap(),
    val selectedAppIds: Set<String> = emptySet(),
    val appSources: Map<String, AppSourceConfig> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val keepAospCounterparts: Set<String> = emptySet(),
    val configVersion: String? = null,
    val createdReleaseId: String? = null,
    val lastBuiltReleaseId: String? = null,
    val dismissedReleaseId: String? = null,
    val packageDefinitions: Map<String, String> = emptyMap(),
    val configSnapshot: String? = null,
    val installerSettings: Map<String, String> = emptyMap()
)

enum class AppSource(val displayName: String) {
    GITLAB("GitLab"),
    DEVICE("Device")
}

data class AppSourceConfig(
    val source: AppSource = AppSource.GITLAB,
    val location: String = ""
)

enum class AndroidVersion(val displayName: String, val apiLevel: Int) {
    ANDROID_10("Android 10", 29),
    ANDROID_11("Android 11", 30),
    ANDROID_12("Android 12", 31),
    ANDROID_12L("Android 12L", 32),
    ANDROID_13("Android 13", 33),
    ANDROID_14("Android 14", 34),
    ANDROID_15("Android 15", 35),
    ANDROID_16("Android 16", 36),
    ANDROID_17("Android 17", 37)
}

enum class Architecture(val displayName: String, val value: String) {
    ARM64("ARM64", "arm64-v8a"),
    ARM("ARM", "armeabi-v7a"),
    X86_64("x86_64", "x86_64"),
    X86("x86", "x86")
}

class BuildProjectRepository(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    fun recoveryBackup(): String? = preferences.getString("recovery_backup", null)
    private fun preserveRecovery(json: String) {
        if (recoveryBackup() != json) {
            val digest = java.security.MessageDigest.getInstance("SHA-256").digest(json.toByteArray())
                .joinToString("") { "%02x".format(it) }
            check(preferences.edit().putString("recovery_backup:$digest", json).putString("recovery_backup", json).commit())
        }
    }

    fun getProjects(): List<BuildProject> = synchronized(WRITE_LOCK) {
        val json = preferences.getString(PROJECTS_KEY, null) ?: return@synchronized emptyList()
        runCatching {
            val projects = JSONArray(json)
            var needsTimestampMigration = false
            val result = recoverRecords((0 until projects.length()).toList(), decode = { index ->
                val project = projects.getJSONObject(index)
                if (!project.has("createdAt")) needsTimestampMigration = true
                BuildProject(
                    id = project.getString("id"),
                    name = project.getString("name"),
                    androidVersion = AndroidVersion.valueOf(project.getString("androidVersion")),
                    architecture = Architecture.valueOf(project.getString("architecture")),
                    selectedAppSetId = project.optString("selectedAppSetId", "core"),
                    selectedPackageAppSets = project.optJSONObject("selectedPackageAppSets")?.let { values ->
                        buildMap { values.keys().forEach { put(it, values.getString(it)) } }
                    }.orEmpty(),
                    defaultChannel = project.optString("defaultChannel", "stable"),
                    channelOverrides = project.optJSONObject("channelOverrides")?.let { values ->
                        buildMap { values.keys().forEach { put(it, values.getString(it)) } }
                    }.orEmpty(),
                    selectedAppIds = project.optJSONArray("selectedAppIds")
                        ?.let { selectedApps ->
                            buildSet {
                                repeat(selectedApps.length()) { appIndex ->
                                    add(selectedApps.getString(appIndex))
                                }
                            }
                        }
                        .orEmpty(),
                    keepAospCounterparts = project.optJSONArray("keepAospCounterparts")?.let { entries ->
                        buildSet { repeat(entries.length()) { add(entries.getString(it)) } }
                    }.orEmpty(),
                    appSources = project.optJSONObject("appSources")
                        ?.let { sources ->
                            buildMap {
                                sources.keys().forEach { appId ->
                                    val config = sources.getJSONObject(appId)
                                    put(
                                        appId,
                                        AppSourceConfig(
                                            source = runCatching {
                                                AppSource.valueOf(config.getString("source"))
                                            }.getOrDefault(AppSource.GITLAB),
                                            location = config.optString("location")
                                        )
                                    )
                                }
                            }
                        }
                        .orEmpty(),
                    configVersion = project.optString("configVersion").takeIf { it.isNotBlank() },
                    createdReleaseId = project.optString("createdReleaseId").takeIf { it.isNotBlank() },
                    lastBuiltReleaseId = project.optString("lastBuiltReleaseId").takeIf { it.isNotBlank() },
                    dismissedReleaseId = project.optString("dismissedReleaseId").takeIf { it.isNotBlank() },
                    configSnapshot = project.optString("configSnapshot").takeIf { it.isNotBlank() },
                    installerSettings = project.optJSONObject("installerSettings")?.let { values ->
                        buildMap { values.keys().forEach { put(it, values.getString(it)) } }
                    }.orEmpty(),
                    packageDefinitions = project.optJSONObject("packageDefinitions")?.let { values ->
                        buildMap { values.keys().forEach { put(it, values.getString(it)) } }
                    }.orEmpty(),
                    createdAt = project.optLong("createdAt", System.currentTimeMillis())
                )
            }, onFailure = { _, _ -> preserveRecovery(json) })
            if (needsTimestampMigration) saveProjects(result) else result
        }.getOrElse { preserveRecovery(json); emptyList() }
    }

    fun addProject(project: BuildProject): List<BuildProject> {
        return synchronized(WRITE_LOCK) { saveProjects(getProjects() + project) }
    }

    fun updateProject(project: BuildProject): List<BuildProject> = mutateProject(project.id) { project }

    fun mutateProject(id: String, update: (BuildProject) -> BuildProject): List<BuildProject> = synchronized(WRITE_LOCK) {
        saveProjects(getProjects().map { existing -> if (existing.id == id) update(existing) else existing })
    }

    fun mutateProjects(update: (List<BuildProject>) -> List<BuildProject>): List<BuildProject> = synchronized(WRITE_LOCK) {
        saveProjects(update(getProjects()))
    }

    fun deleteProject(projectId: String): List<BuildProject> {
        return synchronized(WRITE_LOCK) { saveProjects(getProjects().filterNot { it.id == projectId }) }
    }

    private fun saveProjects(projects: List<BuildProject>): List<BuildProject> {
        val json = JSONArray().apply {
            projects.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("name", it.name)
                        .put("androidVersion", it.androidVersion.name)
                        .put("architecture", it.architecture.name)
                        .put("selectedAppSetId", it.selectedAppSetId)
                        .put("selectedPackageAppSets", JSONObject(it.selectedPackageAppSets))
                        .put("defaultChannel", it.defaultChannel)
                        .put("channelOverrides", JSONObject(it.channelOverrides))
                        .put("createdAt", it.createdAt)
                        .put("configVersion", it.configVersion)
                        .put("createdReleaseId", it.createdReleaseId)
                        .put("lastBuiltReleaseId", it.lastBuiltReleaseId)
                        .put("dismissedReleaseId", it.dismissedReleaseId)
                        .put("packageDefinitions", JSONObject(it.packageDefinitions))
                        .put("configSnapshot", it.configSnapshot)
                        .put("installerSettings", JSONObject(it.installerSettings))
                        .put("selectedAppIds", JSONArray(it.selectedAppIds.toList()))
                        .put("keepAospCounterparts", JSONArray(it.keepAospCounterparts.toList()))
                        .put(
                            "appSources",
                            JSONObject().apply {
                                it.appSources.forEach { (appId, config) ->
                                    put(
                                        appId,
                                        JSONObject()
                                            .put("source", config.source.name)
                                            .put("location", config.location)
                                    )
                                }
                            }
                        )
                )
            }
        }
        preferences.edit().putString(PROJECTS_KEY, json.toString()).apply()
        return projects
    }

    private companion object {
        val WRITE_LOCK = Any()
        const val PREFERENCES_NAME = "build_projects"
        const val PROJECTS_KEY = "projects"
    }
}

class LatestBuildRepository(context: Context) {
    private val preferences = context.getSharedPreferences("latest_project_builds", Context.MODE_PRIVATE)

    fun get(projectId: String): String? = preferences.getString(projectId, null)

    fun save(projectId: String, location: String) {
        preferences.edit().putString(projectId, location).apply()
    }

    fun remove(projectId: String) {
        preferences.edit().remove(projectId).apply()
    }
}

package com.nikgapps.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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
    val createdAt: Long = System.currentTimeMillis()
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

    fun getProjects(): List<BuildProject> {
        val json = preferences.getString(PROJECTS_KEY, null) ?: return emptyList()
        return runCatching {
            val projects = JSONArray(json)
            var needsTimestampMigration = false
            val result = List(projects.length()) { index ->
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
                    createdAt = project.optLong("createdAt", System.currentTimeMillis())
                )
            }
            if (needsTimestampMigration) saveProjects(result) else result
        }.getOrDefault(emptyList())
    }

    fun addProject(project: BuildProject): List<BuildProject> {
        return saveProjects(getProjects() + project)
    }

    fun updateProject(project: BuildProject): List<BuildProject> {
        return saveProjects(
            getProjects().map { existing ->
                if (existing.id == project.id) project else existing
            }
        )
    }

    fun deleteProject(projectId: String): List<BuildProject> {
        return saveProjects(getProjects().filterNot { it.id == projectId })
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
                        .put("selectedAppIds", JSONArray(it.selectedAppIds.toList()))
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
        const val PREFERENCES_NAME = "build_projects"
        const val PROJECTS_KEY = "projects"
    }
}

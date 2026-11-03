package com.nikgapps.app.data

import kotlinx.serialization.json.*
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

internal data class BackupSource(val appVersion: String, val versionCode: Long, val buildType: String)
internal data class ProjectBackup(val source: BackupSource, val exportedAt: String, val projects: List<BuildProject>,
    val formatVersion: Int)

/** Release compatibility is tied to FORMAT_VERSION, not debug/release application IDs or versionCode. */
internal object ProjectBackups {
    const val FORMAT_VERSION = 1
    const val MAX_BYTES = 8 * 1024 * 1024
    private const val SIGNATURE = "nikgapps-project-backup"
    private val json = Json { prettyPrint = true }
    private val idPattern = Regex("[A-Za-z0-9_-]{1,128}")
    private fun digest(value: JsonElement) = MessageDigest.getInstance("SHA-256")
        .digest(value.toString().toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    fun export(projects: List<BuildProject>, source: BackupSource, at: String = Instant.now().toString()): String {
        require(projects.size <= 1000) { "Too many projects in backup" }
        val records = JsonArray(projects.map(::encodeProject))
        val envelope = buildJsonObject {
            put("type", SIGNATURE)
            put("formatVersion", FORMAT_VERSION)
            put("minimumReaderVersion", FORMAT_VERSION)
            put("exportedAt", at)
            put("createdWith", buildJsonObject {
                put("appVersion", source.appVersion); put("versionCode", source.versionCode); put("buildType", source.buildType)
            })
            put("projects", records)
            put("projectsSha256", digest(records))
        }
        return json.encodeToString(JsonObject.serializer(), envelope).also {
            require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup exceeds 8 MiB" }
        }
    }

    fun read(text: String): ProjectBackup {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup exceeds 8 MiB" }
        val root = Json.parseToJsonElement(text.removePrefix("\uFEFF")).jsonObject
        require(root.string("type") == SIGNATURE) { "This is not a NikGapps project backup" }
        val version = root.getValue("formatVersion").jsonPrimitive.int
        val minimum = root.getValue("minimumReaderVersion").jsonPrimitive.int
        require(version >= 1 && minimum >= 1) { "Invalid backup format version" }
        require(version <= FORMAT_VERSION && minimum <= FORMAT_VERSION) {
            "This backup uses format $version. Update NikGapps to a release supporting that backup format."
        }
        val records = root.getValue("projects").jsonArray
        require(records.size <= 1000) { "Too many projects in backup" }
        require(root.string("projectsSha256") == digest(records)) { "Backup is damaged: project checksum does not match" }
        val source = root.getValue("createdWith").jsonObject
        val at = root.string("exportedAt").also { Instant.parse(it) }
        val projects = records.map { decodeProject(it.jsonObject) }
        require(projects.map { it.id }.distinct().size == projects.size) { "Backup contains duplicate project IDs" }
        return ProjectBackup(BackupSource(source.string("appVersion"), source.getValue("versionCode").jsonPrimitive.long,
            source.string("buildType")), at, projects, version)
    }

    /** Full replacement only. Elite members may select projects; keeping existing projects is a future feature. */
    fun replacement(compatible: List<BuildProject>, elite: Boolean,
        selectedIds: Set<String> = compatible.mapTo(hashSetOf()) { it.id }): List<BuildProject> {
        val allIds = compatible.mapTo(hashSetOf()) { it.id }
        require(selectedIds.all { it in allIds }) { "Only compatible projects can be restored" }
        require(elite || selectedIds == allIds) { "Choosing projects to restore requires Elite membership" }
        val selected = compatible.filter { it.id in selectedIds }
        require(selected.isNotEmpty()) { "No compatible projects can be imported. Existing projects were kept." }
        require(compatible.map { it.id }.distinct().size == compatible.size) { "Backup contains duplicate project IDs" }
        require(elite || selected.size <= REGULAR_PROJECT_LIMIT) {
            "This backup contains more than $REGULAR_PROJECT_LIMIT compatible projects. An Elite account is required to restore it. Existing projects were kept."
        }
        return selected.toList()
    }

    private fun encodeProject(p: BuildProject) = buildJsonObject {
        put("id", p.id); put("name", p.name); put("androidVersion", p.androidVersion.name)
        put("architecture", p.architecture.name); put("selectedAppSetId", p.selectedAppSetId)
        put("defaultChannel", p.defaultChannel); put("createdAt", p.createdAt)
        put("selectedPackageAppSets", strings(p.selectedPackageAppSets)); put("channelOverrides", strings(p.channelOverrides))
        put("selectedAppIds", array(p.selectedAppIds)); put("keepAospCounterparts", array(p.keepAospCounterparts))
        put("appSources", buildJsonObject { p.appSources.forEach { (id, config) ->
            // Device paths and URI permissions cannot be transferred between app installations.
            put(id, buildJsonObject { put("source", config.source.name) })
        } })
        p.configVersion?.let { put("configVersion", it) }
        p.createdReleaseId?.let { put("createdReleaseId", it) }
        p.lastBuiltReleaseId?.let { put("lastBuiltReleaseId", it) }
        p.dismissedReleaseId?.let { put("dismissedReleaseId", it) }
        p.configSnapshot?.let { put("configSnapshot", it) }
        put("packageDefinitions", strings(p.packageDefinitions)); put("installerSettings", strings(p.installerSettings))
        put("debloatTargets", array(p.debloatTargets))
    }

    private fun decodeProject(p: JsonObject): BuildProject {
        val id = p.string("id").also { require(it.length == 36); UUID.fromString(it) }
        val name = p.string("name").also {
            require(it.isNotBlank() && it.length <= MAX_PROJECT_NAME_LENGTH && it.none { c -> c.isISOControl() }) {
                "Backup contains an invalid project name"
            }
        }
        val selected = p.list("selectedAppIds").toSet().also { values -> values.forEach(::validateId) }
        val owners = p.map("selectedPackageAppSets").also { it.forEach { (k, v) -> validateId(k); validateId(v) } }
        val defaultChannel = p.string("defaultChannel").also(::validateChannel)
        val overrides = p.map("channelOverrides").also { it.forEach { (k, v) -> validateId(k); validateChannel(v) } }
        val keep = p.list("keepAospCounterparts").toSet().also { it.forEach(::validateId) }
        val targets = p.list("debloatTargets").also { com.nikgapps.app.registry.DebloaterConfig.validate(it) }
        val sourceConfigs = p.getValue("appSources").jsonObject.mapValues { (key, value) ->
            validateId(key)
            AppSourceConfig(AppSource.valueOf(value.jsonObject.string("source")))
        }
        fun optional(key: String) = p[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content?.also {
            require(it.length <= if (key == "configSnapshot") 512 * 1024 else 256) { "Backup field is too long: $key" }
        }
        fun release(key: String) = optional(key)?.also {
            require(it.matches(Regex("[A-Za-z0-9._-]+")) && !it.contains("..")) { "Invalid release identifier" }
        }
        val configVersion = optional("configVersion")?.also { require(it.toIntOrNull()?.let { n -> n > 0 } == true) }
        return BuildProject(id, name, AndroidVersion.valueOf(p.string("androidVersion")),
            Architecture.valueOf(p.string("architecture")),
            selectedAppSetId = p.string("selectedAppSetId").also(::validateId), selectedPackageAppSets = owners,
            defaultChannel = defaultChannel, channelOverrides = overrides, selectedAppIds = selected,
            appSources = sourceConfigs, createdAt = p.getValue("createdAt").jsonPrimitive.long.also { require(it >= 0) },
            keepAospCounterparts = keep, configVersion = configVersion,
            createdReleaseId = release("createdReleaseId"), lastBuiltReleaseId = release("lastBuiltReleaseId"),
            dismissedReleaseId = release("dismissedReleaseId"), packageDefinitions = p.map("packageDefinitions"),
            configSnapshot = optional("configSnapshot"), installerSettings = p.map("installerSettings"), debloatTargets = targets)
    }
    private fun validateId(value: String) { require(idPattern.matches(value)) { "Invalid package or AppSet ID" } }
    private fun validateChannel(value: String) { require(value in setOf("stable", "beta", "canary")) { "Unknown package channel" } }
    private fun strings(values: Map<String, String>) = buildJsonObject { values.forEach { (k, v) -> put(k, v) } }
    private fun array(values: Collection<String>) = JsonArray(values.map(::JsonPrimitive))
    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.let {
        require(it.isString) { "Expected text: $key" }; it.content
    }
    private fun JsonObject.list(key: String) = getValue(key).jsonArray.also {
        require(it.size <= 4096) { "Too many entries: $key" }
    }.map { it.jsonPrimitive.let { v -> require(v.isString); v.content } }
    private fun JsonObject.map(key: String) = getValue(key).jsonObject.also { require(it.size <= 4096) }.mapValues {
        it.value.jsonPrimitive.let { v -> require(v.isString); v.content }
    }
}

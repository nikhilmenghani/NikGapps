package com.nikgapps.app.registry

import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

internal data class PackageIconAsset(val url: String, val sha256: String, val size: Long)

/** Optional metadata: failures must never prevent listing packages or building ZIPs. */
internal object PackageIconRepository {
    private const val BASE = "https://gitlab.com/api/v4/projects/85036487/"
    private const val MAX_ICON_BYTES = 512 * 1024
    private val mutex = Mutex()
    private val client = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()
    private var lastAttempt = 0L
    private var assets: Map<String, PackageIconAsset>? = null

    internal fun parse(text: String): Map<String, PackageIconAsset> {
        val root = Json.parseToJsonElement(text).jsonObject
        require(root["schemaVersion"]?.jsonPrimitive?.int == 1)
        return root.getValue("icons").jsonObject.mapNotNull { (name, raw) ->
            runCatching {
                val entry = raw.jsonObject
                val url = entry.getValue("url").jsonPrimitive.content
                val sha = entry.getValue("sha256").jsonPrimitive.content
                val size = entry.getValue("size").jsonPrimitive.long
                require(name.matches(Regex("[a-zA-Z0-9_.]+")))
                require(sha.matches(Regex("[a-f0-9]{64}")))
                require(size in 1..MAX_ICON_BYTES.toLong())
                // Legacy entries remain usable for already cached icons only.
                val github = url.matches(Regex("https://api\\.github\\.com/repos/nikgapps/nikgapps-packages/releases/assets/[0-9]+"))
                require(github || url.startsWith("${BASE}packages/generic/app-icons/$sha/"))
                require(!url.contains("..") && !url.contains('?') && !url.contains('#'))
                name to PackageIconAsset(url, sha, size)
            }.getOrNull()
        }.toMap()
    }

    private fun get(url: String, limit: Int): ByteArray {
        return client.executeRegistryRequest(Request.Builder().url(url).build(), maxAttempts = 1) { response ->
            response.body.byteStream().use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    check(output.size() + count <= limit)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        }
    }

    private fun sha256(data: ByteArray) = MessageDigest.getInstance("SHA-256")
        .digest(data).joinToString("") { "%02x".format(it) }

    suspend fun icon(context: Context, packageName: String): File? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val directory = File(context.cacheDir, "nikgapps/icons").apply { mkdirs() }
            val manifest = File(directory, "icons.json")
            val now = System.currentTimeMillis()
            if (assets == null || now - lastAttempt > TimeUnit.MINUTES.toMillis(30)) {
                lastAttempt = now
                assets = runCatching {
                    val bytes = get(RegistryDelivery.contents("icons.json"), 1024 * 1024)
                    val parsed = parse(bytes.toString(Charsets.UTF_8))
                    val temporary = File(directory, "icons.json.tmp")
                    temporary.writeBytes(bytes)
                    check(temporary.renameTo(manifest))
                    parsed
                }.getOrElse {
                    assets ?: runCatching { parse(manifest.readText()) }.getOrDefault(emptyMap())
                }
            }
            val asset = assets?.get(packageName) ?: return@withLock null
            val file = File(directory, "${asset.sha256}.img")
            runCatching {
                if (!file.isFile || file.length() != asset.size || sha256(file.readBytes()) != asset.sha256) {
                    val bytes = get(asset.url, MAX_ICON_BYTES)
                    check(bytes.size.toLong() == asset.size && sha256(bytes) == asset.sha256)
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    check(bounds.outWidth in 1..1024 && bounds.outHeight in 1..1024)
                    val temporary = File(directory, "${asset.sha256}.tmp")
                    temporary.writeBytes(bytes)
                    check(temporary.renameTo(file))
                }
                file
            }.getOrNull()
        }
    }
}

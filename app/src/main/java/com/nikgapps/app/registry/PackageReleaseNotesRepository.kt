package com.nikgapps.app.registry

import com.nikgapps.app.utils.network.NetworkClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Date
import java.util.Locale

internal object PackageReleaseNotesRepository {
    // The same package changelog rendered by the NikGapps downloads page.
    private const val URL = "https://raw.githubusercontent.com/nikgapps/nikgapps.github.io/main/_data/changelogs.yaml"
    private const val MAX_BYTES = 256 * 1024
    private const val CACHE_TTL_MILLIS = 5 * 60 * 1000L
    private val cacheMutex = Mutex()
    private val ordinal = Regex("^(\\d{1,2})(?:st|nd|rd|th)\\b", RegexOption.IGNORE_CASE)
    private val dateFormats = listOf(DateTimeFormatter.ISO_LOCAL_DATE) +
        listOf("d MMM uuuu", "d MMMM uuuu").map {
            DateTimeFormatter.ofPattern(it, Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT)
        }

    suspend fun changesOn(cacheDirectory: File, releaseDay: LocalDate, online: Boolean): List<String> =
        withContext(Dispatchers.IO) {
            cacheMutex.withLock {
                val cache = File(cacheDirectory, "nikgapps/package-release-notes.yaml")
                val cached = runCatching {
                    cache.takeIf { it.isFile && it.length() <= MAX_BYTES }?.let { parse(it.readText()) }
                }.getOrNull()
                val cacheAge = System.currentTimeMillis() - cache.lastModified()
                if (!online || cached != null && cacheAge in 0 until CACHE_TTL_MILLIS) {
                    return@withLock cached?.get(releaseDay).orEmpty()
                }
                try {
                    val yamlText = NetworkClient.executeRequest(Request.Builder().url(URL).build()).use { response ->
                        check(response.isSuccessful) { "Unable to load package release notes (${response.code})" }
                        val source = response.body.source()
                        require(!source.request(MAX_BYTES + 1L)) { "Package changelog is too large" }
                        source.readUtf8()
                    }
                    val notes = parse(yamlText)
                    // A cache write failure should not hide a valid response.
                    runCatching {
                        SafeFilePublication.write(cache, { yamlText.byteInputStream() }, { staged ->
                            check(staged.readText() == yamlText) { "Package changelog cache write incomplete" }
                        })
                    }
                    notes[releaseDay].orEmpty()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    cached?.get(releaseDay).orEmpty()
                }
            }
        }

    internal fun parse(text: String): Map<LocalDate, List<String>> {
        val options = LoaderOptions().apply {
            codePointLimit = MAX_BYTES
            maxAliasesForCollections = 0
            nestingDepthLimit = 12
        }
        val entries = Yaml(SafeConstructor(options)).load<Any?>(text) as? List<*>
            ?: error("Invalid package changelog format")
        return buildMap<LocalDate, List<String>> {
            for (entry in entries.filterIsInstance<Map<*, *>>()) {
                val day = parseDate(entry["date"]) ?: continue
                val changes = (entry["changes"] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>()
                    .mapNotNull { (it["item"] as? String)?.trim()?.takeIf(String::isNotEmpty) }
                if (changes.isNotEmpty()) put(day, (get(day).orEmpty() + changes).distinct())
            }
        }
    }

    private fun parseDate(value: Any?): LocalDate? = when (value) {
        is Date -> value.toInstant().atOffset(ZoneOffset.UTC).toLocalDate()
        is String -> {
            val date = ordinal.replace(value.substringBefore('(').trim()) { it.groupValues[1] }
            dateFormats.firstNotNullOfOrNull { runCatching { LocalDate.parse(date, it) }.getOrNull() }
        }
        else -> null
    }
}

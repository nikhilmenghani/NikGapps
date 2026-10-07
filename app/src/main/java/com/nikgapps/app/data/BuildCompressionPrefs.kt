package com.nikgapps.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.nikgapps.app.utils.managers.prefMutableState
import com.nikgapps.app.utils.network.EliteMembershipRepository
import kotlinx.coroutines.CancellationException

object BuildCompressionPrefs {
    var compressed by prefMutableState("compressed_build", false) { booleanPreferencesKey(it) }
    var level by prefMutableState("build_compression_level", 6) { intPreferencesKey(it) }
    var askBeforeBuild by prefMutableState("ask_build_compression", true) { booleanPreferencesKey(it) }
}

fun compressionLevelFor(compressed: Boolean, requestedLevel: Int, elite: Boolean): Int =
    if (!compressed) 0 else if (elite) requestedLevel.coerceIn(1, 9) else 6

suspend fun verifiedCompressionLevel(compressed: Boolean, requestedLevel: Int): Int {
    if (!compressed) return 0
    if (requestedLevel == 6) return 6
    val elite = try {
        EliteMembershipRepository.isElite(GithubPrefs.username)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    }
    return compressionLevelFor(true, requestedLevel, elite)
}

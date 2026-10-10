package com.nikgapps.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.nikgapps.app.utils.managers.prefMutableState
import com.nikgapps.app.utils.network.EliteMembershipRepository
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

object BuildDatePrefs {
    var useCurrentDate by prefMutableState("zip_use_current_date", false) { booleanPreferencesKey(it) }
}

fun zipFilenameTimestamp(useCurrentDate: Boolean, elite: Boolean, today: LocalDate): Instant? =
    if (useCurrentDate && elite) today.atStartOfDay().toInstant(ZoneOffset.UTC) else null

suspend fun verifiedZipFilenameTimestamp(): Instant? {
    if (!BuildDatePrefs.useCurrentDate) return null
    val elite = try { EliteMembershipRepository.isElite(GithubPrefs.username) }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { false }
    return zipFilenameTimestamp(true, elite, LocalDate.now())
}

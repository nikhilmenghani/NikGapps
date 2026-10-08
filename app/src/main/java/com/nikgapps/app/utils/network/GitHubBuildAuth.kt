package com.nikgapps.app.utils.network

import com.nikgapps.app.data.GithubPrefs
import com.nikgapps.app.data.DisplayPrefs
import kotlinx.coroutines.CancellationException
import java.io.IOException

/** Permit explicit guest builds or verify the signed-in GitHub session. */
object GitHubBuildAuth {
    class AuthException(message: String, cause: Throwable? = null) : IOException(message, cause)

    suspend fun requireBuildAccess() {
        val token = GithubPrefs.token
        if (DisplayPrefs.guestAccessEnabled && GithubPrefs.guestMode && token.isBlank()) return
        if (token.isBlank()) throw AuthException("Sign in to GitHub before building a ZIP.")
        try {
            GitHubDeviceAuth.accountProfile(token)
            if (GithubPrefs.token != token) {
                throw AuthException("GitHub account changed. Retry the build.")
            }
        } catch (failure: AuthException) {
            throw failure
        } catch (invalid: GitHubDeviceAuth.InvalidTokenException) {
            if (GithubPrefs.token == token) {
                if (GithubPrefs.username.isNotBlank()) GithubPrefs.lastUsername = GithubPrefs.username
                GithubPrefs.token = ""
                GithubPrefs.username = ""
                GithubPrefs.avatarUrl = ""
            }
            throw AuthException("GitHub access has ended. Sign in again before building a ZIP.", invalid)
        } catch (failure: IOException) {
            throw AuthException("Could not verify GitHub sign-in. Check your connection and retry.", failure)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            throw AuthException("Could not verify GitHub sign-in. Retry the build.", failure)
        }
    }
}

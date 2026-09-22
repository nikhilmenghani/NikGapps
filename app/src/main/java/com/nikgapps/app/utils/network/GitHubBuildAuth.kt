package com.nikgapps.app.utils.network

import com.nikgapps.app.data.GithubPrefs
import kotlinx.coroutines.CancellationException
import java.io.IOException

/** Require a currently valid GitHub session before preparing or assembling a ZIP. */
object GitHubBuildAuth {
    class AuthException(message: String, cause: Throwable? = null) : IOException(message, cause)

    suspend fun requireAuthenticated() {
        val token = GithubPrefs.token
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

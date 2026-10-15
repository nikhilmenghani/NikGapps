package com.nikgapps.app.utils

suspend fun runInstallation(extract: suspend () -> Boolean, mount: suspend () -> Boolean,
    install: suspend () -> Boolean): Boolean {
    check(extract()) { "Extraction failed" }
    check(mount()) { "Mount failed" }
    check(install()) { "Package installation failed" }
    return true
}

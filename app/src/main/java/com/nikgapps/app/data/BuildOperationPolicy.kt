package com.nikgapps.app.data

fun shouldEnqueueBuild(persistedStarted: Boolean, workExists: Boolean, explicitRetry: Boolean): Boolean =
    explicitRetry || (!persistedStarted && !workExists)

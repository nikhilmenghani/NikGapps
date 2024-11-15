package com.nikgapps.app.utils.application

object ApplicationMode {
    fun isInPreviewMode(): Boolean {
        return try {
            Class.forName("androidx.compose.ui.tooling.preview.Preview")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
package com.nikgapps.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nikgapps.app.utils.managers.prefMutableState

enum class GlassBackground(val id: String, val label: String) {
    GOOGLE("google", "Google colors"),
    OCEAN("ocean", "Ocean"),
    FOREST("forest", "Forest"),
    DUSK("dusk", "Dusk"),
    GRAPHITE("graphite", "Graphite");

    companion object {
        fun fromId(id: String): GlassBackground = entries.firstOrNull { it.id == id } ?: GOOGLE
    }
}

object BackgroundPrefs {
    private var selectedId by prefMutableState(
        keyName = "glassBackground",
        defaultValue = GlassBackground.GOOGLE.id,
        getPreferencesKey = { stringPreferencesKey(it) }
    )

    var selected: GlassBackground
        get() = GlassBackground.fromId(selectedId)
        set(value) { selectedId = value.id }
}

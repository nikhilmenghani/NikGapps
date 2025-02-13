package com.nikgapps.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nikgapps.app.utils.managers.emptyString

object SingleText {
    var show by mutableStateOf(false)
    var title = emptyString
        private set
    var description = emptyString
        private set
    var onConfirm: (text: String) -> Unit = {}
        private set
    var text = emptyString

    fun dismiss() {
        show = false
        title = emptyString
        description = emptyString
        text = emptyString
        onConfirm = {}
    }

    fun show(
        title: String,
        description: String,
        text: String,
        onConfirm: (text: String) -> Unit
    ) {
        SingleText.title = title
        SingleText.description = description
        SingleText.text = text
        SingleText.onConfirm = onConfirm
        show = true
    }
}
package com.nikgapps.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nikgapps.app.utils.managers.emptyString

object SingleChoice {
    var show by mutableStateOf(false)

    var title = emptyString
        private set
    var description = emptyString
        private set
    var choices = mutableListOf<String>()
        private set
    var onSelect: (choice: Int) -> Unit = {}
        private set
    var selectedChoice = -1

    fun dismiss() {
        show = false
        title = emptyString
        description = emptyString
        choices.clear()
        selectedChoice = -1
        onSelect = {}
    }

    fun show(
        title: String,
        description: String,
        choices: List<String>,
        selectedChoice: Int,
        onSelect: (choice: Int) -> Unit
    ) {
        SingleChoice.title = title
        SingleChoice.description = description
        SingleChoice.choices.clear()
        SingleChoice.choices.addAll(choices)
        SingleChoice.onSelect = onSelect
        SingleChoice.selectedChoice = selectedChoice
        show = true
    }
}

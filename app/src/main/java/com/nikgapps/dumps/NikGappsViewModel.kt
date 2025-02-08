package com.nikgapps.dumps

import androidx.lifecycle.ViewModel
import com.nikgapps.app.data.AppSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NikGappsViewModel : ViewModel() {
    // Backing property for app sets state
    private val _appSets = MutableStateFlow<List<AppSet>>(emptyList())
    val appSets: StateFlow<List<AppSet>> = _appSets // Expose as read-only

    // Function to update app sets
    fun updateAppSets(newAppSets: List<AppSet>) {
        _appSets.value = newAppSets
    }

    // Optional: Function to clear the app sets
    fun clearAppSets() {
        _appSets.value = emptyList()
    }
}
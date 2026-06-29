package com.contactsnap.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.contactsnap.app.data.HistoryEntry
import com.contactsnap.app.data.HistoryStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val store = HistoryStore(app)

    val entries: StateFlow<List<HistoryEntry>> = store.historyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun delete(id: String) {
        viewModelScope.launch { store.delete(id) }
    }

    fun clear() {
        viewModelScope.launch { store.clear() }
    }
}

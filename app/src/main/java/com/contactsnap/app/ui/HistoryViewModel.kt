package com.contactsnap.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.contactsnap.app.data.HistoryEntry
import com.contactsnap.app.data.HistoryStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A group label and how many scans carry it. */
data class GroupCount(val name: String, val count: Int)

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val store = HistoryStore(app)

    val entries: StateFlow<List<HistoryEntry>> = store.historyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val groups: StateFlow<List<GroupCount>> = store.historyFlow
        .map { list ->
            list.map { it.contact.group }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
                .map { (name, count) -> GroupCount(name, count) }
                .sortedBy { it.name.lowercase() }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun delete(id: String) {
        viewModelScope.launch { store.delete(id) }
    }

    fun clear() {
        viewModelScope.launch { store.clear() }
    }

    fun renameGroup(old: String, new: String) {
        if (new.isBlank() || new.trim() == old) return
        viewModelScope.launch { store.renameGroup(old, new) }
    }

    fun deleteGroup(name: String) {
        viewModelScope.launch { store.deleteGroup(name) }
    }
}

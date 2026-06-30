package com.contactsnap.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.contactsnap.app.contacts.ContactDeduper
import com.contactsnap.app.contacts.DupCluster
import com.contactsnap.app.contacts.DupContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FixUiState(
    val loading: Boolean = false,
    val scanned: Boolean = false,
    val clusters: List<DupCluster> = emptyList(),
    val allLoading: Boolean = false,
    val allLoaded: Boolean = false,
    val allContacts: List<DupContact> = emptyList()
)

class FixContactsViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(FixUiState())
    val state: StateFlow<FixUiState> = _state.asStateFlow()

    fun scan() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val clusters = withContext(Dispatchers.IO) { ContactDeduper.findClusters(getApplication()) }
            _state.update { FixUiState(loading = false, scanned = true, clusters = clusters) }
        }
    }

    fun merge(cluster: DupCluster, keepName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { ContactDeduper.merge(getApplication(), cluster, keepName) }
            if (ok) {
                // Drop the merged cluster from the list immediately.
                _state.update { s -> s.copy(clusters = s.clusters.filterNot { it.id == cluster.id }) }
            }
            onResult(ok)
        }
    }

    /** Loads every device contact so the user can hand-pick two to merge. */
    fun loadAll() {
        _state.update { it.copy(allLoading = true) }
        viewModelScope.launch {
            val all = withContext(Dispatchers.IO) { ContactDeduper.findAll(getApplication()) }
            _state.update { it.copy(allLoading = false, allLoaded = true, allContacts = all) }
        }
    }

    /** Merge two hand-picked contacts into one, keeping [keepName]. */
    fun mergePair(first: DupContact, second: DupContact, keepName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val cluster = ContactDeduper.clusterOf(listOf(first, second))
            val ok = withContext(Dispatchers.IO) { ContactDeduper.merge(getApplication(), cluster, keepName) }
            if (ok) {
                // Reload so both the browse list and any prior duplicate scan stay accurate.
                val all = withContext(Dispatchers.IO) { ContactDeduper.findAll(getApplication()) }
                _state.update { s ->
                    s.copy(
                        allContacts = all,
                        clusters = s.clusters.filterNot { c ->
                            c.contacts.any { it.contactId == first.contactId || it.contactId == second.contactId }
                        }
                    )
                }
            }
            onResult(ok)
        }
    }
}

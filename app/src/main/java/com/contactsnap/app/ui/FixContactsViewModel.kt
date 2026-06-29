package com.contactsnap.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.contactsnap.app.contacts.ContactDeduper
import com.contactsnap.app.contacts.DupCluster
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
    val clusters: List<DupCluster> = emptyList()
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
}

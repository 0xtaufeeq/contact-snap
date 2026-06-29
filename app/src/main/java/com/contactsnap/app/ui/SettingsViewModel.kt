package com.contactsnap.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.contactsnap.app.data.ApiKeyStore
import com.contactsnap.app.util.NameFormat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ApiKeyStore(app)

    val apiKey: StateFlow<String> = store.apiKeyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val nameFormat: StateFlow<NameFormat> = store.nameFormatFlow
        .map { NameFormat.fromId(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, NameFormat.DEFAULT)

    fun save(value: String) {
        viewModelScope.launch { store.set(value) }
    }

    fun setNameFormat(fmt: NameFormat) {
        viewModelScope.launch { store.setNameFormat(fmt.id) }
    }
}

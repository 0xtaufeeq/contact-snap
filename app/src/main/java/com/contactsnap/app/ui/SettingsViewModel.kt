package com.contactsnap.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.contactsnap.app.data.ApiKeyStore
import com.contactsnap.app.util.NameFormat
import com.contactsnap.app.util.ThemeMode
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

    val theme: StateFlow<ThemeMode> = store.themeFlow
        .map { ThemeMode.fromId(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DEFAULT)

    // null = still loading; gates whether onboarding should show.
    val onboardingSeen: StateFlow<Boolean?> = store.onboardingSeenFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun save(value: String) {
        viewModelScope.launch { store.set(value) }
    }

    fun setNameFormat(fmt: NameFormat) {
        viewModelScope.launch { store.setNameFormat(fmt.id) }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { store.setTheme(mode.id) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { store.setOnboardingSeen() }
    }
}

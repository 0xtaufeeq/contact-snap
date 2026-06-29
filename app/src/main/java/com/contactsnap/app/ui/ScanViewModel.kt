package com.contactsnap.app.ui

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.contactsnap.app.contacts.ContactReader
import com.contactsnap.app.contacts.ContactWriter
import com.contactsnap.app.data.ApiKeyStore
import com.contactsnap.app.data.HistoryEntry
import com.contactsnap.app.data.HistoryStore
import com.contactsnap.app.model.ParsedContact
import com.contactsnap.app.net.GeminiContactExtractor
import com.contactsnap.app.net.GeminiException
import com.contactsnap.app.util.ImageBytes
import com.contactsnap.app.util.NameFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class ScanStatus { Idle, Processing, Ready, Error }

data class ScanUiState(
    val status: ScanStatus = ScanStatus.Idle,
    val imageUri: Uri? = null,
    val contact: ParsedContact = ParsedContact(),
    val errorMessage: String? = null,
    val saved: Boolean = false,
    val historyId: String? = null,
    val imagePath: String = "",
    val lowConfidence: Set<String> = emptySet()
)

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val extractor = GeminiContactExtractor()
    private val settings = ApiKeyStore(app)
    private val historyStore = HistoryStore(app)

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private var lastUris: List<Uri> = emptyList()

    /** Single image (gallery import). */
    fun onImageCaptured(uri: Uri) = onImagesCaptured(listOf(uri))

    /** One or more images of the SAME card (e.g. front + back); extracted together. */
    fun onImagesCaptured(uris: List<Uri>) {
        if (uris.isEmpty()) return
        lastUris = uris
        val id = UUID.randomUUID().toString()
        _state.update {
            it.copy(
                status = ScanStatus.Processing, imageUri = uris.first(),
                saved = false, errorMessage = null, historyId = id, imagePath = "", lowConfidence = emptySet()
            )
        }
        viewModelScope.launch {
            try {
                val key = settings.get()
                val (extraction, savedPath) = withContext(Dispatchers.IO) {
                    val b64s = uris.map { ImageBytes.toBase64Jpeg(getApplication(), it) }
                    val ex = extractor.extract(b64s, key)
                    val path = runCatching { ImageBytes.saveScanCopy(getApplication(), uris.first(), id) }.getOrNull()
                    ex to path.orEmpty()
                }
                _state.update {
                    it.copy(
                        status = ScanStatus.Ready, contact = extraction.contact,
                        imagePath = savedPath, lowConfidence = extraction.lowConfidence
                    )
                }
                upsertHistory()
            } catch (e: GeminiException) {
                _state.update { it.copy(status = ScanStatus.Error, errorMessage = e.message) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(status = ScanStatus.Error, errorMessage = e.message ?: "Something went wrong.")
                }
            }
        }
    }

    /** Re-run extraction on the current image(s) (e.g. after fixing the API key). */
    fun retry() {
        if (lastUris.isNotEmpty()) onImagesCaptured(lastUris)
    }

    /** Reopen a contact from history into the review flow. */
    fun loadFromHistory(entry: HistoryEntry) {
        _state.value = ScanUiState(
            status = ScanStatus.Ready,
            contact = entry.contact,
            historyId = entry.id,
            imagePath = entry.imagePath,
            imageUri = entry.imagePath.takeIf { it.isNotBlank() }?.let { File(it).toUri() }
        )
    }

    fun updateContact(transform: (ParsedContact) -> ParsedContact) {
        _state.update { it.copy(contact = transform(it.contact)) }
    }

    /** Persist current edits (group, fixes) back into the history entry. */
    fun syncHistory() {
        if (_state.value.historyId == null || _state.value.status != ScanStatus.Ready) return
        upsertHistory()
    }

    private fun upsertHistory() {
        val s = _state.value
        val id = s.historyId ?: return
        viewModelScope.launch {
            runCatching {
                historyStore.upsert(
                    HistoryEntry(
                        id = id,
                        timestamp = System.currentTimeMillis(),
                        contact = s.contact,
                        imagePath = s.imagePath
                    )
                )
            }
        }
    }

    /** Find existing contacts that look like the current one (runs on IO). */
    fun checkDuplicates(onResult: (List<ContactReader.Match>) -> Unit) {
        viewModelScope.launch {
            val matches = withContext(Dispatchers.IO) {
                ContactReader.findMatches(getApplication(), _state.value.contact)
            }
            onResult(matches)
        }
    }

    /** Append the current contact's details to an existing contact. */
    fun merge(contactId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                ContactWriter.merge(getApplication(), contactId, _state.value.contact)
            }
            if (ok) {
                _state.update { it.copy(saved = true) }
                upsertHistory()
            }
            onResult(ok)
        }
    }

    fun save(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val fmt = NameFormat.fromId(settings.getNameFormat())
            val ok = withContext(Dispatchers.IO) {
                ContactWriter.save(getApplication(), _state.value.contact, fmt)
            }
            if (ok) {
                _state.update { it.copy(saved = true) }
                upsertHistory()
            }
            onResult(ok)
        }
    }

    fun reset() {
        _state.value = ScanUiState()
    }
}

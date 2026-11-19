package com.example.talynxa.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.talynxa.data.PreferencesRepository
import com.example.talynxa.data.model.MatchMode
import com.example.talynxa.data.model.NoteFile
import com.example.talynxa.data.model.NoteItem
import com.example.talynxa.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NoteUiState(
    val currentNote: NoteFile = NoteFile(),
    val currentUri: Uri? = null,
    val searchQuery: String = "",
    val selectedTags: Set<String> = emptySet(),
    val matchMode: MatchMode = MatchMode.ALL,
    val isLoading: Boolean = false,
    val isOledTheme: Boolean = false,
    val isDarkThemeOverride: Boolean? = null, // null: follow system, true: dark, false: light
    val isModified: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class NoteViewModel @JvmOverloads constructor(
    application: Application = Application(),
    private val repository: NoteRepository = NoteRepository()
) : AndroidViewModel(application) {

    private val preferencesRepository by lazy { PreferencesRepository(application) }

    private val _uiState = MutableStateFlow(NoteUiState(currentNote = repository.getDefaultNote()))
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    init {
        // Load persisted preferences on startup
        viewModelScope.launch {
            try {
                preferencesRepository.oledThemeFlow.collect { oled ->
                    _uiState.value = _uiState.value.copy(isOledTheme = oled)
                }
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                preferencesRepository.darkThemeOverrideFlow.collect { override ->
                    _uiState.value = _uiState.value.copy(isDarkThemeOverride = override)
                }
            } catch (_: Exception) { }
        }
    }

    val tagCounts: StateFlow<Map<String, Int>> = _uiState
        .map { state -> calculateTagCounts(state.currentNote.content) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = calculateTagCounts(_uiState.value.currentNote.content)
        )

    val filteredContent: StateFlow<List<NoteItem>> = _uiState
        .map { state ->
            filterItems(
                items = state.currentNote.content,
                query = state.searchQuery,
                selectedTags = state.selectedTags,
                matchMode = state.matchMode
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = filterItems(
                items = _uiState.value.currentNote.content,
                query = _uiState.value.searchQuery,
                selectedTags = _uiState.value.selectedTags,
                matchMode = _uiState.value.matchMode
            )
        )

    fun loadFromIntent(context: Context, intent: Intent?) {
        val note = repository.readNoteFromIntent(context, intent)
        val uri = if (intent?.action == Intent.ACTION_VIEW) intent.data else null
        _uiState.value = _uiState.value.copy(
            currentNote = note,
            currentUri = uri,
            selectedTags = emptySet(),
            searchQuery = "",
            isModified = false,
            errorMessage = null
        )
    }

    fun loadFromUri(context: Context, uri: Uri) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        repository.readNoteFromUri(context, uri)
            .onSuccess { note ->
                _uiState.value = _uiState.value.copy(
                    currentNote = note,
                    currentUri = uri,
                    isLoading = false,
                    isModified = false,
                    infoMessage = "Note loaded successfully"
                )
            }
            .onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load file: ${error.localizedMessage}"
                )
            }
    }

    fun loadFromJsonString(jsonString: String) {
        repository.parseJsonString(jsonString)
            .onSuccess { note ->
                _uiState.value = _uiState.value.copy(
                    currentNote = note,
                    currentUri = null,
                    isModified = false,
                    infoMessage = "JSON loaded"
                )
            }
            .onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Invalid JSON: ${error.localizedMessage}"
                )
            }
    }

    fun saveToUri(context: Context, uri: Uri, onPermissionFailure: () -> Unit = {}) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        // Ensure persistable permission if possible
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (_: Exception) { }

        repository.writeNoteToUri(context, uri, _uiState.value.currentNote)
            .onSuccess {
                _uiState.value = _uiState.value.copy(
                    currentUri = uri,
                    isLoading = false,
                    isModified = false,
                    infoMessage = "Saved successfully"
                )
            }
            .onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to save file: ${error.localizedMessage}. Please use Save As."
                )
                onPermissionFailure()
            }
    }

    fun exportToJsonString(): String {
        return repository.encodeJsonString(_uiState.value.currentNote)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleTagSelection(tag: String) {
        val currentTags = _uiState.value.selectedTags.toMutableSet()
        if (currentTags.contains(tag)) {
            currentTags.remove(tag)
        } else {
            currentTags.add(tag)
        }
        _uiState.value = _uiState.value.copy(selectedTags = currentTags)
    }

    fun clearTagSelection() {
        _uiState.value = _uiState.value.copy(selectedTags = emptySet())
    }

    fun setMatchMode(mode: MatchMode) {
        _uiState.value = _uiState.value.copy(matchMode = mode)
    }

    fun toggleOledTheme() {
        val newOled = !_uiState.value.isOledTheme
        _uiState.value = _uiState.value.copy(isOledTheme = newOled)
        viewModelScope.launch {
            preferencesRepository.setOledTheme(newOled)
        }
    }

    fun setDarkThemeOverride(override: Boolean?) {
        _uiState.value = _uiState.value.copy(isDarkThemeOverride = override)
        viewModelScope.launch {
            preferencesRepository.setDarkThemeOverride(override)
        }
    }

    fun updateNoteDetails(name: String) {
        val updatedNote = _uiState.value.currentNote.copy(
            name = name
        )
        _uiState.value = _uiState.value.copy(
            currentNote = updatedNote,
            isModified = true
        )
    }

    // CRUD Operations for NoteItem

    fun addItem(text: String, tags: List<String> = emptyList(), links: List<String> = emptyList()) {
        val cleanTags = tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val cleanLinks = links.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val nextId = (_uiState.value.currentNote.content.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: -1) + 1
        val newItem = NoteItem(id = nextId.toString(), text = text, tags = cleanTags, links = cleanLinks)
        val updatedContent = _uiState.value.currentNote.content + newItem
        val updatedNote = _uiState.value.currentNote.copy(content = updatedContent)
        _uiState.value = _uiState.value.copy(
            currentNote = updatedNote,
            isModified = true
        )
    }

    fun editItem(id: String, text: String, tags: List<String>, links: List<String>) {
        val cleanTags = tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val cleanLinks = links.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val updatedContent = _uiState.value.currentNote.content.map { item ->
            if (item.id == id) {
                item.copy(text = text, tags = cleanTags, links = cleanLinks)
            } else {
                item
            }
        }
        val updatedNote = _uiState.value.currentNote.copy(content = updatedContent)
        _uiState.value = _uiState.value.copy(
            currentNote = updatedNote,
            isModified = true
        )
    }

    fun addTagToItem(itemId: String, tag: String) {
        val cleanTag = tag.trim()
        if (cleanTag.isEmpty()) return
        val updatedContent = _uiState.value.currentNote.content.map { item ->
            if (item.id == itemId) {
                if (!item.tags.contains(cleanTag)) {
                    item.copy(tags = item.tags + cleanTag)
                } else {
                    item
                }
            } else {
                item
            }
        }
        val updatedNote = _uiState.value.currentNote.copy(content = updatedContent)
        _uiState.value = _uiState.value.copy(
            currentNote = updatedNote,
            isModified = true
        )
    }

    fun removeTagFromItem(itemId: String, tag: String) {
        val updatedContent = _uiState.value.currentNote.content.map { item ->
            if (item.id == itemId) {
                item.copy(tags = item.tags.filter { it != tag })
            } else {
                item
            }
        }
        val updatedNote = _uiState.value.currentNote.copy(content = updatedContent)
        _uiState.value = _uiState.value.copy(
            currentNote = updatedNote,
            isModified = true
        )
    }

    fun deleteItem(itemId: String) {
        val updatedContent = _uiState.value.currentNote.content.filter { it.id != itemId }
        val updatedNote = _uiState.value.currentNote.copy(content = updatedContent)
        _uiState.value = _uiState.value.copy(
            currentNote = updatedNote,
            isModified = true
        )
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val content = _uiState.value.currentNote.content.toMutableList()
        if (fromIndex !in content.indices || toIndex !in content.indices) return
        val item = content.removeAt(fromIndex)
        content.add(toIndex, item)
        val updatedNote = _uiState.value.currentNote.copy(content = content)
        _uiState.value = _uiState.value.copy(
            currentNote = updatedNote,
            isModified = true
        )
    }

    fun moveItemUp(itemId: String) {
        val content = _uiState.value.currentNote.content
        val index = content.indexOfFirst { it.id == itemId }
        if (index > 0) {
            moveItem(index, index - 1)
        }
    }

    fun moveItemDown(itemId: String) {
        val content = _uiState.value.currentNote.content
        val index = content.indexOfFirst { it.id == itemId }
        if (index >= 0 && index < content.size - 1) {
            moveItem(index, index + 1)
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
    }

    companion object {
        fun calculateTagCounts(content: List<NoteItem>): Map<String, Int> {
            val counts = mutableMapOf<String, Int>()
            for (item in content) {
                for (tag in item.tags) {
                    val trimmed = tag.trim()
                    if (trimmed.isNotEmpty()) {
                        counts[trimmed] = (counts[trimmed] ?: 0) + 1
                    }
                }
            }
            return counts.toSortedMap()
        }

        fun filterItems(
            items: List<NoteItem>,
            query: String,
            selectedTags: Set<String>,
            matchMode: MatchMode
        ): List<NoteItem> {
            val trimmedQuery = query.trim()
            val cleanSelectedTags = selectedTags.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

            return items.filter { item ->
                // Search query match
                val matchesQuery = trimmedQuery.isEmpty() ||
                        item.text.contains(trimmedQuery, ignoreCase = true) ||
                        item.tags.any { it.contains(trimmedQuery, ignoreCase = true) }

                if (!matchesQuery) return@filter false

                // Tag filter match
                if (cleanSelectedTags.isEmpty()) {
                    true
                } else {
                    val itemTagSet = item.tags.map { it.trim().lowercase() }.toSet()
                    when (matchMode) {
                        MatchMode.ALL -> cleanSelectedTags.all { tag -> itemTagSet.contains(tag) }
                        MatchMode.ANY -> cleanSelectedTags.any { tag -> itemTagSet.contains(tag) }
                    }
                }
            }
        }
    }
}

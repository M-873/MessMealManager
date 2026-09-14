package com.example.messmealmanager.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.messmealmanager.auth.AuthManager
import com.example.messmealmanager.data.FirestoreRepository
import com.example.messmealmanager.model.Member
import com.example.messmealmanager.model.Sheet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class NameValidationState {
    object Idle : NameValidationState()
    object Checking : NameValidationState()
    object Available : NameValidationState()
    data class Taken(val message: String = "You already own a sheet with this name") : NameValidationState()
    data class Invalid(val message: String = "Sheet name cannot be blank") : NameValidationState()
}

class HomeViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val authManager: AuthManager
) : ViewModel() {

    val currentUser = authManager.currentUser
    val currentUserId = currentUser?.uid ?: ""

    // Real-time stream of the current member's profile
    val currentMember: StateFlow<Member?> = firestoreRepository
        .getMemberFlow(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val currentUserName: String
        get() = currentMember.value?.name?.ifBlank { currentUser?.displayName ?: "User" }
            ?: (currentUser?.displayName ?: "User")

    // Real-time stream of sheets where the user is an editor or owner
    val userSheets: StateFlow<List<Sheet>> = firestoreRepository
        .getSheetsFlowForUser(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Sheet>>(emptyList())
    val searchResults: StateFlow<List<Sheet>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    // Create Sheet Dialog state
    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _newSheetName = MutableStateFlow("")
    val newSheetName: StateFlow<String> = _newSheetName.asStateFlow()

    private val _nextSheetNumber = MutableStateFlow(1)
    val nextSheetNumber: StateFlow<Int> = _nextSheetNumber.asStateFlow()

    private val _nameValidationState = MutableStateFlow<NameValidationState>(NameValidationState.Idle)
    val nameValidationState: StateFlow<NameValidationState> = _nameValidationState.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _createError = MutableStateFlow<String?>(null)
    val createError: StateFlow<String?> = _createError.asStateFlow()

    private var validationJob: Job? = null

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        _isSearching.value = true
        searchJob = viewModelScope.launch {
            delay(300) // Debounce typing
            try {
                val results = firestoreRepository.searchSheets(query)
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun openCreateDialog() {
        _newSheetName.value = ""
        _nameValidationState.value = NameValidationState.Idle
        _createError.value = null
        _showCreateDialog.value = true

        viewModelScope.launch {
            val nextNumber = firestoreRepository.getNextSheetNumber()
            _nextSheetNumber.value = nextNumber
        }
    }

    fun closeCreateDialog() {
        _showCreateDialog.value = false
        _newSheetName.value = ""
        _nameValidationState.value = NameValidationState.Idle
        _createError.value = null
    }

    fun onSheetNameChanged(name: String) {
        _newSheetName.value = name
        validationJob?.cancel()

        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _nameValidationState.value = NameValidationState.Invalid()
            return
        }

        _nameValidationState.value = NameValidationState.Checking
        validationJob = viewModelScope.launch {
            delay(250) // Debounce validation
            try {
                val isTaken = firestoreRepository.isSheetNameTaken(trimmed, currentUserId)
                if (isTaken) {
                    _nameValidationState.value = NameValidationState.Taken()
                } else {
                    _nameValidationState.value = NameValidationState.Available
                }
            } catch (e: Exception) {
                _nameValidationState.value = NameValidationState.Idle
            }
        }
    }

    fun createSheet(onSuccess: (Sheet) -> Unit) {
        val sheetName = _newSheetName.value.trim()
        if (sheetName.isBlank() || _nameValidationState.value !is NameValidationState.Available) {
            return
        }

        _isCreating.value = true
        _createError.value = null

        viewModelScope.launch {
            try {
                val newSheet = firestoreRepository.createSheet(
                    sheetName = sheetName,
                    ownerId = currentUserId,
                    ownerName = currentUserName
                )
                _isCreating.value = false
                closeCreateDialog()
                onSuccess(newSheet)
            } catch (e: Exception) {
                _isCreating.value = false
                _createError.value = e.message ?: "Failed to create sheet"
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        authManager.signOut()
        onSignedOut()
    }
}

package com.example.messmealmanager.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.messmealmanager.auth.AuthManager
import com.example.messmealmanager.data.FirestoreRepository
import com.example.messmealmanager.model.Member
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val authManager: AuthManager
) : ViewModel() {

    val firebaseUser = authManager.currentUser
    val currentUserId = firebaseUser?.uid ?: ""

    private val _currentMember = MutableStateFlow<Member?>(null)
    val currentMember: StateFlow<Member?> = _currentMember.asStateFlow()

    private val _displayNameInput = MutableStateFlow("")
    val displayNameInput: StateFlow<String> = _displayNameInput.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow<String?>(null)
    val saveSuccess: StateFlow<String?> = _saveSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadMemberProfile()
    }

    private fun loadMemberProfile() {
        if (currentUserId.isBlank()) return
        viewModelScope.launch {
            firestoreRepository.getMemberFlow(currentUserId).collect { member ->
                _currentMember.value = member
                if (member != null && _displayNameInput.value.isBlank()) {
                    _displayNameInput.value = member.name.ifBlank { firebaseUser?.displayName ?: "" }
                }
            }
        }
    }

    fun onDisplayNameChanged(newName: String) {
        _displayNameInput.value = newName
        _saveSuccess.value = null
        _errorMessage.value = null
    }

    fun saveDisplayName() {
        val trimmed = _displayNameInput.value.trim()
        if (trimmed.isBlank()) {
            _errorMessage.value = "Display name cannot be empty"
            return
        }

        _isSaving.value = true
        _errorMessage.value = null
        _saveSuccess.value = null

        viewModelScope.launch {
            try {
                firestoreRepository.updateMemberName(currentUserId, trimmed)
                _saveSuccess.value = "Display name updated successfully!"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update display name"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        authManager.signOut()
        onSignedOut()
    }

    fun clearMessages() {
        _saveSuccess.value = null
        _errorMessage.value = null
    }
}

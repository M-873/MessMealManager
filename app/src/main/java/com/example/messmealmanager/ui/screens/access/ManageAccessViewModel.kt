package com.example.messmealmanager.ui.screens.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.messmealmanager.auth.AuthManager
import com.example.messmealmanager.data.FirestoreRepository
import com.example.messmealmanager.model.Member
import com.example.messmealmanager.model.MemberRole
import com.example.messmealmanager.model.Sheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MemberWithRole(
    val member: Member,
    val role: MemberRole
)

class ManageAccessViewModel(
    val sheetId: String,
    private val firestoreRepository: FirestoreRepository,
    private val authManager: AuthManager
) : ViewModel() {

    val currentUserId = authManager.currentUser?.uid ?: ""

    private val _sheet = MutableStateFlow<Sheet?>(null)
    val sheet: StateFlow<Sheet?> = _sheet.asStateFlow()

    private val _membersWithRole = MutableStateFlow<List<MemberWithRole>>(emptyList())
    val membersWithRole: StateFlow<List<MemberWithRole>> = _membersWithRole.asStateFlow()

    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()

    private val _searchEmail = MutableStateFlow("")
    val searchEmail: StateFlow<String> = _searchEmail.asStateFlow()

    private val _foundMember = MutableStateFlow<Member?>(null)
    val foundMember: StateFlow<Member?> = _foundMember.asStateFlow()

    private val _selectedRoleToAdd = MutableStateFlow(MemberRole.EDITOR)
    val selectedRoleToAdd: StateFlow<MemberRole> = _selectedRoleToAdd.asStateFlow()

    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadSheet()
    }

    private fun loadSheet() {
        viewModelScope.launch {
            firestoreRepository.getSheetFlow(sheetId).collect { loadedSheet ->
                _sheet.value = loadedSheet
                if (loadedSheet != null) {
                    _isOwner.value = loadedSheet.ownerId == currentUserId
                    loadAllMembers(loadedSheet)
                }
            }
        }
    }

    private fun loadAllMembers(sheet: Sheet) {
        viewModelScope.launch {
            try {
                val allMemberIds = (listOf(sheet.ownerId) + sheet.editors + sheet.viewers).distinct()
                val memberMap = firestoreRepository.getMembersByIds(allMemberIds).associateBy { it.userId }

                val result = allMemberIds.map { id ->
                    val memberObj = memberMap[id] ?: Member(
                        userId = id,
                        name = if (id == sheet.ownerId) sheet.ownerName.ifEmpty { "Owner" } else "User ($id)"
                    )
                    val role = when {
                        id == sheet.ownerId -> MemberRole.OWNER
                        sheet.editors.contains(id) -> MemberRole.EDITOR
                        else -> MemberRole.VIEWER
                    }
                    MemberWithRole(member = memberObj, role = role)
                }
                _membersWithRole.value = result
            } catch (e: Exception) {
                _membersWithRole.value = listOf(
                    MemberWithRole(
                        Member(userId = sheet.ownerId, name = sheet.ownerName.ifEmpty { "Owner" }),
                        MemberRole.OWNER
                    )
                )
            }
        }
    }

    fun onEmailChanged(email: String) {
        _searchEmail.value = email
        _hasSearched.value = false
        _foundMember.value = null
        _errorMessage.value = null
    }

    fun onRoleToAddChanged(role: MemberRole) {
        _selectedRoleToAdd.value = role
    }

    fun searchMember() {
        val email = _searchEmail.value.trim().lowercase()
        if (email.isBlank()) {
            _errorMessage.value = "Please enter an email address"
            return
        }

        _isSearching.value = true
        _hasSearched.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val member = firestoreRepository.findMemberByEmail(email)
                _foundMember.value = member
                if (member == null) {
                    _errorMessage.value = "No registered user found with email \"$email\"."
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to search for member"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun addMemberWithRole(member: Member, role: MemberRole) {
        val currentSheet = _sheet.value ?: return
        if (!_isOwner.value) {
            _errorMessage.value = "Only the owner can manage permissions"
            return
        }

        if (member.userId == currentSheet.ownerId) {
            _errorMessage.value = "${member.name} is the owner of this sheet"
            return
        }

        _isUpdating.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val isEditor = role == MemberRole.EDITOR
                firestoreRepository.setUserSheetRole(sheetId, member.userId, isEditor)
                val roleName = if (isEditor) "Edit access" else "View access"
                _successMessage.value = "Granted $roleName to ${member.name.ifEmpty { member.email }}"
                _searchEmail.value = ""
                _foundMember.value = null
                _hasSearched.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to grant access"
            } finally {
                _isUpdating.value = false
            }
        }
    }

    fun toggleMemberRole(userId: String) {
        val currentSheet = _sheet.value ?: return
        if (!_isOwner.value) {
            _errorMessage.value = "Only the owner can modify permissions"
            return
        }

        if (userId == currentSheet.ownerId) {
            _errorMessage.value = "Cannot change the role of the sheet owner"
            return
        }

        val isCurrentlyEditor = currentSheet.editors.contains(userId)
        val newIsEditor = !isCurrentlyEditor

        _isUpdating.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                firestoreRepository.setUserSheetRole(sheetId, userId, newIsEditor)
                val newRoleName = if (newIsEditor) "Edit access" else "View access"
                _successMessage.value = "Updated role to $newRoleName"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update role"
            } finally {
                _isUpdating.value = false
            }
        }
    }

    fun removeMember(userId: String) {
        val currentSheet = _sheet.value ?: return
        if (!_isOwner.value) {
            _errorMessage.value = "Only the owner can remove members"
            return
        }

        if (userId == currentSheet.ownerId) {
            _errorMessage.value = "Cannot remove the sheet owner"
            return
        }

        _isUpdating.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                firestoreRepository.removeUserFromSheet(sheetId, userId)
                _successMessage.value = "Member removed successfully"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to remove member"
            } finally {
                _isUpdating.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
